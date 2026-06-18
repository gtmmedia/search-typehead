import React, { useState, useEffect } from 'react';
import { fetchMetrics, fetchBatchStats } from '../services/api';

const MetricsPanel = ({ refreshTrigger }) => {
    const [metrics, setMetrics] = useState(null);
    const [batchStats, setBatchStats] = useState(null);

    useEffect(() => {
        const loadMetrics = async () => {
            try {
                const [m, b] = await Promise.all([fetchMetrics(), fetchBatchStats()]);
                setMetrics(m);
                setBatchStats(b);
            } catch (err) {
                console.error("Failed to fetch metrics", err);
            }
        };
        loadMetrics();
        const interval = setInterval(loadMetrics, 5000);
        return () => clearInterval(interval);
    }, [refreshTrigger]);

    if (!metrics || !batchStats) return <div className="card loading">Loading metrics...</div>;

    return (
        <div className="card metrics-panel">
            <h2>System Health</h2>
            
            <div className="metrics-grid">
                <div className="metric-box">
                    <span className="metric-label">Avg Latency</span>
                    <span className="metric-value">{metrics.averageLatencyMs} ms</span>
                </div>
                <div className="metric-box">
                    <span className="metric-label">p50 Latency</span>
                    <span className="metric-value">{metrics.p50LatencyMs} ms</span>
                </div>
                <div className="metric-box">
                    <span className="metric-label">p95 Latency</span>
                    <span className="metric-value highlight">{metrics.p95LatencyMs} ms</span>
                </div>
            </div>

            <div className="metrics-grid">
                <div className="metric-box">
                    <span className="metric-label">Cache Hit Rate</span>
                    <span className="metric-value success">{(parseFloat(metrics.cacheHitRatio) * 100).toFixed(1)}%</span>
                </div>
                <div className="metric-box">
                    <span className="metric-label">Cache Miss Rate</span>
                    <span className="metric-value">{(parseFloat(metrics.cacheMissRate) * 100).toFixed(1)}%</span>
                </div>
            </div>

            <div className="metrics-grid full-width">
                <div className="metric-box accent">
                    <span className="metric-label">Batch Write Reduction</span>
                    <span className="metric-value">{batchStats.writeReduction.toFixed(2)}%</span>
                    <span className="metric-subtext">
                        ({batchStats.batchedWrites} writes instead of {batchStats.rawWrites})
                    </span>
                </div>
            </div>
        </div>
    );
};

export default MetricsPanel;
