import api from './api';
import type { Companion, PaginatedResponse } from '@/types';

export interface CreateCompanionRequest {
  name: string;
  description: string;
  personality: {
    traits: string[];
    specialties: string[];
    tone: string;
    customPrompt?: string;
  };
  style: string;
}

export const companionService = {
  async getAll(page = 0, size = 20): Promise<PaginatedResponse<Companion>> {
    const response = await api.get<PaginatedResponse<Companion>>('/companions', {
      params: { page, size },
    });
    return response.data;
  },

  async getById(id: string): Promise<Companion> {
    const response = await api.get<Companion>(`/companions/${id}`);
    return response.data;
  },

  async create(data: CreateCompanionRequest): Promise<Companion> {
    const response = await api.post<Companion>('/companions', data);
    return response.data;
  },

  async update(id: string, data: Partial<CreateCompanionRequest>): Promise<Companion> {
    const response = await api.put<Companion>(`/companions/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/companions/${id}`);
  },
};
