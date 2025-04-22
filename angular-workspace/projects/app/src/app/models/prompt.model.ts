export interface Prompt {
  promptUuid?: string;
  userId?: string;
  name: string;
  description: string;
  promptText: string;
  outputSchema: string;
  createTimestamp?: Date;
  updateTimestamp?: Date;
}