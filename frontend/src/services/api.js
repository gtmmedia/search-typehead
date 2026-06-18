import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 5000,
});

export const fetchSuggestions = async (query) => {
    const response = await api.get(`/suggest?q=${encodeURIComponent(query)}`);
    return response.data.suggestions || [];
};

export const submitSearch = async (query) => {
    const response = await api.post('/search', { query });
    return response.data;
};

export const fetchTrending = async (mode = 'trending') => {
    const response = await api.get(`/trending?mode=${mode}`);
    return response.data || [];
};

export const fetchMetrics = async () => {
    const response = await api.get('/metrics');
    return response.data;
};

export const fetchBatchStats = async () => {
    const response = await api.get('/batch/stats');
    return response.data;
};

export default api;
