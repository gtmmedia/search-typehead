import React, { useState, useEffect } from 'react';
import { fetchTrending } from '../services/api';

const TrendingSection = ({ refreshTrigger }) => {
    const [trending, setTrending] = useState([]);
    const [mode, setMode] = useState('trending');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadTrending = async () => {
            setLoading(true);
            try {
                const data = await fetchTrending(mode);
                setTrending(data);
            } catch (err) {
                console.error("Failed to load trending data", err);
            } finally {
                setLoading(false);
            }
        };
        loadTrending();
        
        const interval = setInterval(loadTrending, 5000); // refresh every 5s
        return () => clearInterval(interval);
    }, [mode, refreshTrigger]);

    return (
        <div className="card trending-section">
            <div className="card-header">
                <h2>Trending Searches</h2>
                <div className="toggle-group">
                    <button 
                        className={mode === 'trending' ? 'active' : ''} 
                        onClick={() => setMode('trending')}
                    >
                        Viral
                    </button>
                    <button 
                        className={mode === 'historical' ? 'active' : ''} 
                        onClick={() => setMode('historical')}
                    >
                        All Time
                    </button>
                </div>
            </div>
            
            {loading && trending.length === 0 ? (
                <div className="loader centered"></div>
            ) : (
                <div className="trending-list">
                    {trending.map((item, index) => (
                        <div key={item.query} className="trending-item">
                            <div className="rank">#{index + 1}</div>
                            <div className="query">{item.query}</div>
                            <div className="score">{(item.score).toFixed(2)}</div>
                        </div>
                    ))}
                    {trending.length === 0 && <p className="empty-text">No trending data yet.</p>}
                </div>
            )}
        </div>
    );
};

export default TrendingSection;
