import React, { useState } from 'react';
import SearchBar from '../components/SearchBar';
import TrendingSection from '../components/TrendingSection';
import MetricsPanel from '../components/MetricsPanel';

const Home = () => {
    const [refreshTrigger, setRefreshTrigger] = useState(0);

    const handleSearchSuccess = () => {
        // Trigger a refresh of metrics and trending when a new search is successfully submitted
        setRefreshTrigger(prev => prev + 1);
    };

    return (
        <div className="home-container">
            <header className="header">
                <h1>Search Typeahead System</h1>
                <p>Lightning fast, distributed, and scalable.</p>
            </header>
            
            <main className="main-content">
                <section className="search-section">
                    <SearchBar onSearchSuccess={handleSearchSuccess} />
                </section>
                
                <section className="dashboard-section">
                    <TrendingSection refreshTrigger={refreshTrigger} />
                    <MetricsPanel refreshTrigger={refreshTrigger} />
                </section>
            </main>
        </div>
    );
};

export default Home;
