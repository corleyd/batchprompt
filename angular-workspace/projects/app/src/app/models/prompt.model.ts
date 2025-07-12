export interface Prompt {
  promptUuid?: string;
  userId?: string;
  name: string;
  description: string;
  promptText: string;
  outputMethod?: 'TEXT' | 'STRUCTURED' | 'BOTH';
  responseTextColumnName?: string;
  responseJsonSchema: string;
  temperature?: number;
  maxTokens?: number;
  createTimestamp?: Date;
  updateTimestamp?: Date;
  lastJobRunTimestamp?: Date;
  jobRunCount?: number;
}