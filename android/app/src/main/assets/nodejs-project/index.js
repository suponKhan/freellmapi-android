#!/usr/bin/env node
/**
 * FreeLLMAPI Android - Server Entry Point
 * 
 * A simplified OpenAI-compatible LLM router that runs on Android via nodejs-mobile.
 * Supports keyless providers (Pollinations, ApiAirforce) and optional API keys
 * for additional providers (Groq, Cerebras, etc.).
 * 
 * Environment variables:
 *   FREELLMAPI_PORT     - Port to listen on (default: 3001)
 *   FREELLMAPI_HOST     - Host to bind (default: 0.0.0.0)
 *   FREELLMAPI_API_KEY  - Unified API key for auth (default: freellmapi)
 *   GROQ_API_KEY        - Groq API key (optional)
 *   CEREBRAS_API_KEY    - Cerebras API key (optional)
 *   GEMINI_API_KEY      - Google Gemini API key (optional)
 *   OPENROUTER_API_KEY  - OpenRouter API key (optional)
 */

import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import http from 'http';
import { readFileSync, writeFileSync, existsSync } from 'fs';
import { join } from 'path';
import { fileURLToPath } from 'url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const PORT = parseInt(process.env.FREELLMAPI_PORT || '3001', 10);
const HOST = process.env.FREELLMAPI_HOST || '0.0.0.0';
const API_KEY = process.env.FREELLMAPI_API_KEY || 'freellmapi';
const DATA_DIR = join(__dirname, 'data');

// Create data directory if needed
if (!existsSync(DATA_DIR)) {
  try {
    require('fs').mkdirSync(DATA_DIR, { recursive: true });
  } catch (e) {
    // Ignore - Android may have restricted access
  }
}

// ===== Provider Configuration =====
const PROVIDERS = [
  {
    id: 'pollinations',
    name: 'Pollinations.ai',
    baseUrl: 'https://text.pollinations.ai/openai',
    apiKey: null,
    models: [
      { id: 'openai-fast', name: 'GPT-OSS 20B', context: 131000, capabilities: ['tools'] },
      { id: 'openai-protection', name: 'GPT-OSS Protection', context: 131000, capabilities: [] },
      { id: 'openai-default', name: 'GPT-OSS Default', context: 131000, capabilities: [] },
    ],
    priority: 1,
    keyless: true,
  },
  {
    id: 'apiairforce',
    name: 'ApiAirforce',
    baseUrl: 'https://llm.api.airforce/v1',
    apiKey: null,
    models: [
      { id: 'grok-4.1-mini:free', name: 'Grok 4.1 Mini', context: 131000, capabilities: [] },
      { id: 'step-3.5-flash:free', name: 'Step 3.5 Flash', context: 131000, capabilities: ['tools'] },
      { id: 'gemma3-270m:free', name: 'Gemma3 270M', context: 8192, capabilities: [] },
    ],
    priority: 2,
    keyless: true,
  },
  {
    id: 'groq',
    name: 'Groq',
    baseUrl: 'https://api.groq.com/openai/v1',
    apiKey: process.env.GROQ_API_KEY || null,
    models: [
      { id: 'groq/llama-3.3-70b-versatile', name: 'Llama 3.3 70B', context: 131000, capabilities: ['tools'] },
      { id: 'groq/llama-3.1-8b-instant', name: 'Llama 3.1 8B', context: 131000, capabilities: [] },
      { id: 'groq/qwen3-30b-a3b', name: 'Qwen3 30B', context: 131000, capabilities: ['tools'] },
      { id: 'groq/gpt-oss-120b', name: 'GPT-OSS 120B', context: 131000, capabilities: ['tools'] },
    ],
    priority: 3,
    keyless: false,
  },
  {
    id: 'cerebras',
    name: 'Cerebras',
    baseUrl: 'https://api.cerebras.ai/v1',
    apiKey: process.env.CEREBRAS_API_KEY || null,
    models: [
      { id: 'cerebras/llama-3.3-70b', name: 'Llama 3.3 70B', context: 131000, capabilities: ['tools'] },
      { id: 'cerebras/llama-3.1-8b', name: 'Llama 3.1 8B', context: 131000, capabilities: [] },
      { id: 'cerebras/qwen3-235b', name: 'Qwen3 235B', context: 131000, capabilities: ['tools'] },
    ],
    priority: 4,
    keyless: false,
  },
  {
    id: 'gemini',
    name: 'Google Gemini',
    baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai/',
    apiKey: process.env.GEMINI_API_KEY || null,
    models: [
      { id: 'gemini/gemini-2.5-flash', name: 'Gemini 2.5 Flash', context: 1000000, capabilities: ['tools', 'vision'] },
      { id: 'gemini/gemini-2.5-pro', name: 'Gemini 2.5 Pro', context: 1000000, capabilities: ['tools', 'vision'] },
    ],
    priority: 5,
    keyless: false,
  },
  {
    id: 'ollamacloud',
    name: 'Ollama Cloud',
    baseUrl: 'https://api.ollama.com/v1',
    apiKey: process.env.OLLAMA_API_KEY || null,
    models: [
      { id: 'ollama/gpt-oss:120b', name: 'GPT-OSS 120B', context: 131000, capabilities: ['tools'] },
      { id: 'ollama/qwen3:32b', name: 'Qwen3 32B', context: 131000, capabilities: ['tools'] },
    ],
    priority: 6,
    keyless: false,
  },
];

// ===== State =====
const state = {
  activeKey: API_KEY,
  requestLog: [],
  modelCache: new Map(),
  cooldowns: new Map(),
};

// ===== Utilities =====
function logRequest(provider, model, durationMs, success, error) {
  const entry = {
    time: new Date().toISOString(),
    provider,
    model,
    durationMs,
    success,
    error: error?.message,
  };
  state.requestLog.push(entry);
  // Keep only last 1000 entries
  if (state.requestLog.length > 1000) {
    state.requestLog = state.requestLog.slice(-1000);
  }
}

function getCooldown(providerId) {
  const cooldown = state.cooldowns.get(providerId);
  if (!cooldown) return 0;
  const elapsed = Date.now() - cooldown.startedAt;
  if (elapsed >= cooldown.duration) {
    state.cooldowns.delete(providerId);
    return 0;
  }
  return cooldown.remaining;
}

function setCooldown(providerId, durationMs = 60000) {
  state.cooldowns.set(providerId, {
    startedAt: Date.now(),
    duration: durationMs,
    remaining: durationMs,
  });
}

function getAvailableProviders() {
  return PROVIDERS.filter(p => {
    // Keyless providers are always available
    if (p.keyless) return true;
    // Keyed providers need an API key
    return p.apiKey !== null;
  }).sort((a, b) => a.priority - b.priority);
}

function resolveModel(modelName) {
  // Handle special model names
  if (modelName === 'auto' || modelName === 'auto:fast' || modelName === 'auto:smart') {
    const available = getAvailableProviders();
    if (available.length === 0) return null;
    // Pick first available provider's first model
    return {
      providerId: available[0].id,
      modelId: available[0].models[0].id,
    };
  }
  
  // Try to find exact model match
  for (const provider of PROVIDERS) {
    if (provider.keyless || provider.apiKey) {
      const model = provider.models.find(m => 
        modelName === m.id || modelName === modelIdToName(m.id) || 
        modelName.includes(m.id.split('/')[1] || m.id)
      );
      if (model) {
        return { providerId: provider.id, modelId: model.id };
      }
    }
  }
  
  // Try partial match
  for (const provider of PROVIDERS) {
    if (provider.keyless || provider.apiKey) {
      const model = provider.models.find(m => 
        modelName.toLowerCase().includes(m.id.toLowerCase().split('/')[1] || '')
      );
      if (model) {
        return { providerId: provider.id, modelId: model.id };
      }
    }
  }
  
  return null;
}

function modelIdToName(modelId) {
  for (const provider of PROVIDERS) {
    const model = provider.models.find(m => m.id === modelId);
    if (model) return model.name;
  }
  return modelId;
}

// ===== HTTP Client =====
async function callProvider(provider, model, messages, stream = false) {
  const url = `${provider.baseUrl}/chat/completions`;
  const body = {
    model: model.id,
    messages,
    stream,
    temperature: 0.7,
    max_tokens: 4096,
  };
  
  const headers = {
    'Content-Type': 'application/json',
  };
  
  if (provider.apiKey) {
    headers['Authorization'] = `Bearer ${provider.apiKey}`;
  }
  
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 60000);
  
  try {
    const startTime = Date.now();
    const response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    const duration = Date.now() - startTime;
    
    if (!response.ok) {
      const errorText = await response.text();
      logRequest(provider.id, model.id, duration, false, new Error(`HTTP ${response.status}: ${errorText.slice(0, 100)}`));
      
      if (response.status === 429) {
        setCooldown(provider.id, 30000);
      }
      throw new Error(`Provider ${provider.id} returned ${response.status}`);
    }
    
    logRequest(provider.id, model.id, duration, true);
    
    if (stream) {
      // Return the response body directly for streaming
      return response;
    }
    
    const data = await response.json();
    return data;
  } catch (error) {
    clearTimeout(timeout);
    throw error;
  }
}

// ===== Express App =====
const app = express();
app.use(cors());
app.use(express.json({ limit: '25mb' }));

// Auth middleware
app.use('/v1', (req, res, next) => {
  const auth = req.headers.authorization;
  if (!auth || !auth.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing API key' });
  }
  const key = auth.slice(7);
  if (key !== state.activeKey) {
    return res.status(403).json({ error: 'Invalid API key' });
  }
  next();
});

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    uptime: process.uptime(),
    providers: getAvailableProviders().length,
    activeKey: state.activeKey,
  });
});

// OpenAI-compatible endpoints
app.post('/v1/chat/completions', async (req, res) => {
  const { model, messages, stream = false } = req.body;
  
  if (!messages || !Array.isArray(messages)) {
    return res.status(400).json({ error: 'Invalid request: messages is required' });
  }
  
  const resolved = resolveModel(model || 'auto');
  if (!resolved) {
    return res.status(400).json({
      error: `Model '${model || 'auto'}' not found. Available models:`,
      providers: getAvailableProviders().map(p => ({
        id: p.id,
        models: p.models.map(m => m.id),
      })),
    });
  }
  
  const provider = PROVIDERS.find(p => p.id === resolved.providerId);
  if (!provider) {
    return res.status(500).json({ error: 'Provider not found' });
  }
  
  // Check cooldown
  const cooldown = getCooldown(resolved.providerId);
  if (cooldown > 0) {
    return res.status(429).json({
      error: `Provider ${provider.name} is rate-limited. Retry in ${Math.ceil(cooldown / 1000)}s`,
      cooldown,
    });
  }
  
  try {
    if (stream) {
      // Streaming response
      res.setHeader('Content-Type', 'text/event-stream');
      res.setHeader('Cache-Control', 'no-cache');
      res.setHeader('Connection', 'keep-alive');
      
      const response = await callProvider(provider, { id: resolved.modelId }, messages, true);
      
      // Pipe the response
      response.body.on('data', (chunk) => {
        res.write(chunk);
      });
      response.body.on('end', () => {
        res.end();
      });
      response.body.on('error', (err) => {
        console.error('Stream error:', err);
        res.end();
      });
    } else {
      // Non-streaming response
      const data = await callProvider(provider, { id: resolved.modelId }, messages, false);
      res.json(data);
    }
  } catch (error) {
    console.error('Provider error:', error.message);
    
    // Try failover to next provider
    const available = getAvailableProviders().filter(p => p.priority > provider.priority);
    if (available.length > 0) {
      const nextProvider = available[0];
      console.log(`Failover to ${nextProvider.name}...`);
      const nextResolved = resolveModel(model || 'auto');
      if (nextResolved) {
        const nextProviderObj = PROVIDERS.find(p => p.id === nextResolved.providerId);
        const data = await callProvider(nextProviderObj, { id: nextResolved.modelId }, messages, false);
        data.headers = { 'x-routed-via': `${provider.id} -> ${nextProvider.id}` };
        res.json(data);
        return;
      }
    }
    
    res.status(500).json({
      error: error.message,
      hint: 'Try setting API keys for additional providers',
    });
  }
});

app.get('/v1/models', (req, res) => {
  const providers = getAvailableProviders();
  const models = [];
  
  for (const provider of providers) {
    for (const model of provider.models) {
      models.push({
        id: model.id,
        object: 'model',
        created: Math.floor(Date.now() / 1000),
        owned_by: provider.id,
        permission: [],
      });
    }
  }
  
  res.json({
    object: 'list',
    data: models,
  });
});

// Analytics endpoint
app.get('/v1/analytics', (req, res) => {
  res.json({
    requestLog: state.requestLog.slice(-100),
    stats: {
      totalRequests: state.requestLog.length,
      successRate: state.requestLog.length > 0
        ? (state.requestLog.filter(r => r.success).length / state.requestLog.length * 100).toFixed(1) + '%'
        : '0%',
    },
  });
});

// Serve dashboard
app.get('/', (req, res) => {
  res.send(`
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>FreeLLMAPI</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { 
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: #0f0f0f;
      color: #e0e0e0;
      min-height: 100vh;
      padding: 20px;
    }
    .container { max-width: 800px; margin: 0 auto; }
    h1 { color: #6750A4; margin-bottom: 8px; }
    .subtitle { color: #888; margin-bottom: 24px; }
    .card { 
      background: #1e1e1e; 
      border-radius: 12px; 
      padding: 16px; 
      margin-bottom: 16px;
      border: 1px solid #333;
    }
    .card h2 { font-size: 14px; color: #888; margin-bottom: 12px; text-transform: uppercase; letter-spacing: 1px; }
    .provider { 
      display: flex; 
      justify-content: space-between; 
      align-items: center;
      padding: 12px;
      background: #252525;
      border-radius: 8px;
      margin-bottom: 8px;
    }
    .provider-name { font-weight: 600; }
    .provider-status { 
      font-size: 12px; 
      padding: 4px 8px; 
      border-radius: 4px;
      background: ${p => p.active ? '#4CAF50' : '#666'};
    }
    .endpoint { 
      background: #252525; 
      padding: 12px; 
      border-radius: 8px;
      font-family: monospace;
      font-size: 12px;
      word-break: break-all;
      margin-bottom: 8px;
    }
    .copy-btn {
      background: #6750A4;
      color: white;
      border: none;
      padding: 8px 16px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 14px;
    }
    .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
    .stat { text-align: center; padding: 16px; background: #252525; border-radius: 8px; }
    .stat-value { font-size: 24px; font-weight: bold; color: #6750A4; }
    .stat-label { font-size: 12px; color: #888; margin-top: 4px; }
    code { background: #252525; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
  </style>
</head>
<body>
  <div class="container">
    <h1>🤖 FreeLLMAPI</h1>
    <p class="subtitle">OpenAI-compatible LLM Router for Android</p>
    
    <div class="card">
      <h2>Status</h2>
      <div class="stats">
        <div class="stat">
          <div class="stat-value" id="uptime">0s</div>
          <div class="stat-label">Uptime</div>
        </div>
        <div class="stat">
          <div class="stat-value" id="requests">0</div>
          <div class="stat-label">Requests</div>
        </div>
        <div class="stat">
          <div class="stat-value" id="providers">0</div>
          <div class="stat-label">Providers</div>
        </div>
      </div>
    </div>
    
    <div class="card">
      <h2>Endpoints</h2>
      <div class="endpoint">http://localhost:3001/v1/chat/completions</div>
      <div class="endpoint">http://localhost:3001/v1/models</div>
      <div class="endpoint">http://localhost:3001/</div>
      <button class="copy-btn" onclick="copyEndpoint()">Copy Base URL</button>
    </div>
    
    <div class="card">
      <h2>Available Providers</h2>
      <div id="providers-list"></div>
    </div>
    
    <div class="card">
      <h2>Usage Example</h2>
      <pre style="background: #252525; padding: 12px; border-radius: 8px; overflow-x: auto; font-size: 12px;"><code>from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:3001/v1",
    api_key="freellmapi"
)

response = client.chat.completions.create(
    model="auto",
    messages=[{"role": "user", "content": "Hello!"}]
)
print(response.choices[0].message.content)</code></pre>
    </div>
  </div>
  
  <script>
    let startTime = Date.now();
    
    async function updateStats() {
      try {
        const resp = await fetch('/health');
        const data = await resp.json();
        document.getElementById('uptime').textContent = Math.floor(data.uptime) + 's';
        document.getElementById('providers').textContent = data.providers;
      } catch (e) {}
    }
    
    async function loadProviders() {
      try {
        const resp = await fetch('/v1/models');
        const data = await resp.json();
        const list = document.getElementById('providers-list');
        list.innerHTML = data.data.map(m => `
          <div class="provider">
            <span class="provider-name">${m.id}</span>
            <span class="provider-status">✓ Active</span>
          </div>
        `).join('');
      } catch (e) {}
    }
    
    function copyEndpoint() {
      navigator.clipboard.writeText('http://localhost:3001/v1');
      alert('Copied: http://localhost:3001/v1');
    }
    
    setInterval(updateStats, 5000);
    updateStats();
    loadProviders();
    
    // Update request count
    setInterval(async () => {
      try {
        const resp = await fetch('/v1/analytics');
        const data = await resp.json();
        document.getElementById('requests').textContent = data.stats.totalRequests;
      } catch (e) {}
    }, 5000);
  </script>
</body>
</html>
  `);
});

// ===== Start Server =====
const server = app.listen(PORT, HOST, () => {
  console.log(`FreeLLMAPI running on http://${HOST}:${PORT}`);
  console.log(`API Key: ${state.activeKey}`);
  console.log(`Available providers: ${getAvailableProviders().length}`);
  console.log(`Endpoints:`);
  console.log(`  Chat:    http://${HOST}:${PORT}/v1/chat/completions`);
  console.log(`  Models:  http://${HOST}:${PORT}/v1/models`);
  console.log(`  Dashboard: http://${HOST}:${PORT}/`);
});

process.on('SIGTERM', () => {
  console.log('Shutting down...');
  server.close(() => process.exit(0));
});

process.on('uncaughtException', (err) => {
  console.error('Uncaught exception:', err);
});