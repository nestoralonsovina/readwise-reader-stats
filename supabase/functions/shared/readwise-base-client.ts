// Readwise Base Client Implementation

import type { ReadwiseClient, ReadwiseListParams, ReadwiseListResponse } from './readwise-types.ts';

export class ReadwiseBaseClient implements ReadwiseClient {
  private baseUrl = 'https://readwise.io/api/v3';
  private accessToken: string;

  constructor(accessToken: string) {
    this.accessToken = accessToken;
  }

  private getHeaders(): Record<string, string> {
    return {
      'Authorization': `Token ${this.accessToken}`,
      'Content-Type': 'application/json',
    };
  }

  private buildQueryString(params: ReadwiseListParams): string {
    const searchParams = new URLSearchParams();
    
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined) {
        if (key === 'withHtmlContent') {
          searchParams.append(key, value.toString());
        } else {
          searchParams.append(key, value as string);
        }
      }
    });

    return searchParams.toString();
  }

  async listDocuments(params: ReadwiseListParams = {}): Promise<ReadwiseListResponse> {
    const queryString = this.buildQueryString(params);
    const url = `${this.baseUrl}/list/${queryString ? `?${queryString}` : ''}`;

    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: this.getHeaders(),
      });

      if (!response.ok) {
        throw new Error(`Readwise API error: ${response.status} ${response.statusText}`);
      }

      return await response.json() as ReadwiseListResponse;
    } catch (error) {
      throw new Error(`Failed to fetch documents: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }
}

export function createReadwiseClient(accessToken: string): ReadwiseClient {
  return new ReadwiseBaseClient(accessToken);
}