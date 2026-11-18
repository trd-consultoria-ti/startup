import { Logger } from '@config/logger.config';
import {
  AgentContext,
  AgentResult,
  AgentType,
  SwarmConfig,
  SwarmTopology,
  DEFAULT_SWARM_CONFIG,
  DEFAULT_AGENT_TEAM,
} from './agent-types';
import { CoordinatorAgent, CoordinatorResult } from './coordinator.agent';
import { BaseAgent } from './base.agent';
import { ConfigService } from '@config/env.config';

export class AgentOrchestrator {
  private readonly logger = new Logger('AgentOrchestrator');
  private readonly coordinator: CoordinatorAgent;
  private readonly config: SwarmConfig;
  private activeAgentCount = 0;
  private readonly agentMetrics = new Map<string, { count: number; totalTimeMs: number; errors: number }>();

  constructor(configService?: ConfigService, prismaRepository?: any) {
    this.config = DEFAULT_SWARM_CONFIG;
    this.coordinator = new CoordinatorAgent(configService, prismaRepository);

    this.logger.log(
      `[Orchestrator] Swarm initialized | topology=${this.config.topology} | maxAgents=${this.config.maxAgents} | strategy=${this.config.strategy}`,
    );
  }

  getCoordinator(): CoordinatorAgent {
    return this.coordinator;
  }

  listAgents(): BaseAgent[] {
    return this.coordinator.listAgents();
  }

  getSwarmStatus(): {
    topology: SwarmTopology;
    maxAgents: number;
    strategy: string;
    activeAgents: number;
    agents: { type: string; name: string; status: string; capabilities: string[] }[];
  } {
    const agents = this.coordinator.listAgents().map((a) => ({
      type: a.type,
      name: a.name,
      status: a.getStatus(),
      capabilities: a.capabilities,
    }));

    return {
      topology: this.config.topology,
      maxAgents: this.config.maxAgents,
      strategy: this.config.strategy,
      activeAgents: this.activeAgentCount,
      agents,
    };
  }

  async execute(context: AgentContext): Promise<CoordinatorResult> {
    this.activeAgentCount++;
    this.logger.log(
      `[Orchestrator] Execute | topology=${this.config.topology} | remoteJid=${context.remoteJid}`,
    );

    const execStart = Date.now();
    let result: CoordinatorResult;
    try {
      switch (this.config.topology) {
        case 'hierarchical':
          result = await this.executeHierarchical(context);
          break;
        case 'pipeline':
          result = await this.executePipeline(context);
          break;
        case 'parallel':
          result = await this.executeParallel(context);
          break;
        case 'mesh':
          result = await this.executeMesh(context);
          break;
        default:
          result = await this.executeHierarchical(context);
      }
      this.recordAgentMetric(result.agentUsed || 'unknown', Date.now() - execStart, false);
      return result;
    } catch (err) {
      this.recordAgentMetric('orchestrator', Date.now() - execStart, true);
      throw err;
    } finally {
      this.activeAgentCount--;
    }
  }

  private recordAgentMetric(agentType: string, processingTimeMs: number, isError = false): void {
    const existing = this.agentMetrics.get(agentType) || { count: 0, totalTimeMs: 0, errors: 0 };
    existing.count++;
    existing.totalTimeMs += processingTimeMs;
    if (isError) existing.errors++;
    this.agentMetrics.set(agentType, existing);
  }

  getAgentMetrics(): Record<string, { count: number; totalTimeMs: number; errors: number; avgTimeMs: number }> {
    const result: Record<string, { count: number; totalTimeMs: number; errors: number; avgTimeMs: number }> = {};
    for (const [key, val] of this.agentMetrics.entries()) {
      result[key] = {
        count: val.count,
        totalTimeMs: val.totalTimeMs,
        errors: val.errors,
        avgTimeMs: val.count > 0 ? Math.round(val.totalTimeMs / val.count) : 0,
      };
    }
    return result;
  }

  private async executeHierarchical(context: AgentContext): Promise<CoordinatorResult> {
    this.logger.log(`[Orchestrator] Hierarchical mode → Coordinator routing`);
    return await this.coordinator.route(context);
  }

  private async executePipeline(context: AgentContext): Promise<CoordinatorResult> {
    this.logger.log(`[Orchestrator] Pipeline mode → Sequential phases`);

    const phases: { name: string; fn: (ctx: AgentContext) => Promise<AgentResult | null> }[] = [
      {
        name: 'scheduler',
        fn: async (ctx) => {
          const agent = this.coordinator.getAgent('scheduler');
          if (agent && agent.canHandle(ctx)) return await agent.execute(ctx);
          return null;
        },
      },
      {
        name: 'websearch',
        fn: async (ctx) => {
          const agent = this.coordinator.getAgent('websearch');
          if (agent && agent.canHandle(ctx)) return await agent.execute(ctx);
          return null;
        },
      },
      {
        name: 'preprocess',
        fn: async (ctx) => {
          const audioAgent = this.coordinator.getAgent('audio');
          if (audioAgent && audioAgent.canHandle(ctx)) return await audioAgent.execute(ctx);
          const docAgent = this.coordinator.getAgent('document');
          if (docAgent && docAgent.canHandle(ctx)) return await docAgent.execute(ctx);
          return null;
        },
      },
    ];

    let currentContext = { ...context };
    let webContext = '';

    for (const phase of phases) {
      const result = await phase.fn(currentContext);
      if (result && result.status === 'completed') {
        if (phase.name === 'scheduler') {
          return {
            finalResponse: result.response,
            claudeTextSessionId: context.claudeTextSessionId,
            claudeVisionSessionId: context.claudeVisionSessionId,
            conversationHistory: context.conversationHistory,
            agentUsed: 'scheduler',
            webContext: '',
            metadata: {
              agentsInvolved: ['scheduler'],
              processingTime: result.metadata?.processingTime || 0,
              topology: this.config.topology,
              strategy: this.config.strategy,
              ...result.metadata,
            },
          };
        } else if (phase.name === 'websearch') {
          webContext = result.response;
          currentContext.webContext = webContext;
        } else if (phase.name === 'preprocess') {
          currentContext.content = result.response;
        }
      }
    }

    return await this.coordinator.route(currentContext);
  }

  private async executeParallel(context: AgentContext): Promise<CoordinatorResult> {
    this.logger.log(`[Orchestrator] Parallel mode → Concurrent agents`);

    const tasks: Promise<AgentResult | null>[] = [];
    const taskNames: string[] = [];

    const webSearchAgent = this.coordinator.getAgent('websearch');
    if (webSearchAgent && webSearchAgent.canHandle(context)) {
      tasks.push(webSearchAgent.execute(context));
      taskNames.push('websearch');
    }

    const audioAgent = this.coordinator.getAgent('audio');
    if (audioAgent && audioAgent.canHandle(context)) {
      tasks.push(audioAgent.execute(context));
      taskNames.push('audio');
    }

    const docAgent = this.coordinator.getAgent('document');
    if (docAgent && docAgent.canHandle(context)) {
      tasks.push(docAgent.execute(context));
      taskNames.push('document');
    }

    if (tasks.length === 0) {
      return await this.coordinator.route(context);
    }

    const results = await Promise.allSettled(tasks);
    let processedContent = context.content;
    let webContext = '';

    for (let i = 0; i < results.length; i++) {
      const result = results[i];
      if (result.status === 'fulfilled' && result.value && result.value.status === 'completed') {
        if (taskNames[i] === 'websearch') {
          webContext = result.value.response;
        } else if (taskNames[i] === 'audio' || taskNames[i] === 'document') {
          processedContent = result.value.response;
        }
      }
    }

    const updatedContext: AgentContext = {
      ...context,
      content: processedContent,
      webContext,
    };

    return await this.coordinator.route(updatedContext);
  }

  private async executeMesh(context: AgentContext): Promise<CoordinatorResult> {
    this.logger.log(`[Orchestrator] Mesh mode → Peer coordination with delegation`);

    const coordinator = this.coordinator;
    const agents = coordinator.listAgents();

    const capable = agents.filter(
      (a) => !a.isCircuitOpen?.() && a.canHandle(context) && a.type !== 'coordinator' && a.type !== 'summary' && a.type !== 'intent',
    );

    if (capable.length === 0) {
      return await this.coordinator.route(context);
    }

    if (capable.length === 1) {
      const agent = capable[0];
      const result = await agent.execute(context);
      return {
        finalResponse: result.response,
        conversationHistory: context.conversationHistory,
        agentUsed: agent.type,
        metadata: {
          agentsInvolved: [agent.type],
          processingTime: 0,
          topology: this.config.topology,
          strategy: this.config.strategy,
          ...result.metadata,
        },
      };
    }

    const tasks = capable.map(async (agent) => {
      try {
        const result = await agent.execute(context);
        return { agentType: agent.type, result, success: result.status === 'completed' };
      } catch (err: any) {
        return { agentType: agent.type, result: null, success: false, error: err.message };
      }
    });

    const results = await Promise.allSettled(tasks);

    let bestResult: AgentResult | null = null;
    let bestAgent: AgentType = 'text';
    let bestScore = -1;

    for (const r of results) {
      if (r.status === 'fulfilled' && r.value?.success && r.value?.result) {
        const score = (r.value.result.response || '').length;
        if (score > bestScore) {
          bestScore = score;
          bestResult = r.value.result;
          bestAgent = r.value.agentType;
        }
      }
    }

    if (bestResult) {
      return {
        finalResponse: bestResult.response,
        conversationHistory: context.conversationHistory,
        agentUsed: bestAgent,
        metadata: {
          agentsInvolved: capable.map((a) => a.type),
          processingTime: 0,
          topology: this.config.topology,
          strategy: this.config.strategy,
          ...bestResult.metadata,
        },
      };
    }

    return await this.coordinator.route(context);
  }

  getMemory() {
    return this.coordinator.getMemory();
  }

  getTeamConfig() {
    return DEFAULT_AGENT_TEAM;
  }
}
