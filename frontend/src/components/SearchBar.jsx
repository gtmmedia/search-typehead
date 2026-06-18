import React, { useState, useEffect, useRef } from 'react';
import { useDebounce } from '../hooks/useDebounce';
import { fetchSuggestions, submitSearch } from '../services/api';
import SuggestionDropdown from './SuggestionDropdown';

const SearchBar = ({ onSearchSuccess }) => {
    const [query, setQuery] = useState('');
    const [suggestions, setSuggestions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [isFocused, setIsFocused] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    
    const debouncedQuery = useDebounce(query, 300);
    const containerRef = useRef(null);

    useEffect(() => {
        const getSuggestions = async () => {
            if (!debouncedQuery.trim()) {
                setSuggestions([]);
                return;
            }
            setLoading(true);
            setError(null);
            try {
                const results = await fetchSuggestions(debouncedQuery);
                setSuggestions(results);
                setSelectedIndex(-1);
            } catch (err) {
                setError('Failed to fetch suggestions');
                setSuggestions([]);
            } finally {
                setLoading(false);
            }
        };

        getSuggestions();
    }, [debouncedQuery]);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (containerRef.current && !containerRef.current.contains(event.target)) {
                setIsFocused(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleKeyDown = (e) => {
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setSelectedIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : prev));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setSelectedIndex((prev) => (prev > 0 ? prev - 1 : -1));
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
                handleSearch(suggestions[selectedIndex].query);
            } else if (query.trim()) {
                handleSearch(query);
            }
        } else if (e.key === 'Escape') {
            setIsFocused(false);
        }
    };

    const handleSearch = async (searchQuery) => {
        if (!searchQuery.trim()) return;
        setQuery(searchQuery);
        setIsFocused(false);
        try {
            await submitSearch(searchQuery);
            if (onSearchSuccess) onSearchSuccess();
            setQuery(''); // Clear after success
        } catch (err) {
            console.error('Failed to submit search:', err);
        }
    };

    return (
        <div className="search-container" ref={containerRef}>
            <div className="search-input-wrapper">
                <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="11" cy="11" r="8" />
                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <input
                    type="text"
                    className="search-input"
                    placeholder="Search anything..."
                    value={query}
                    onChange={(e) => {
                        setQuery(e.target.value);
                        setIsFocused(true);
                    }}
                    onFocus={() => setIsFocused(true)}
                    onKeyDown={handleKeyDown}
                />
                {loading && <div className="loader"></div>}
            </div>

            {isFocused && (query.trim().length > 0) && (
                <SuggestionDropdown 
                    suggestions={suggestions} 
                    selectedIndex={selectedIndex}
                    onSelect={handleSearch}
                    error={error}
                />
            )}
        </div>
    );
};

export default SearchBar;
