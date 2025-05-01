export interface Prompt {
  promptUuid?: string;
  userId?: string;
  name: string;
  description: string;
  promptText: string;
  outputMethod?: 'TEXT' | 'STRUCTURED' | 'BOTH';
  responseTextColumnName?: string;
  responseJsonSchema: string;
  createTimestamp?: Date;
  updateTimestamp?: Date;
}