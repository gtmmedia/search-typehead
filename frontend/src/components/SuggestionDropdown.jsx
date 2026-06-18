import React from 'react';

const SuggestionDropdown = ({ suggestions, selectedIndex, onSelect, error }) => {
    if (error) {
        return (
            <div className="suggestion-dropdown error">
                <p>{error}</p>
            </div>
        );
    }

    if (!suggestions || suggestions.length === 0) {
        return (
            <div className="suggestion-dropdown empty">
                <p>No suggestions found.</p>
            </div>
        );
    }

    return (
        <ul className="suggestion-dropdown">
            {suggestions.map((item, index) => (
                <li
                    key={item.query}
                    className={`suggestion-item ${index === selectedIndex ? 'active' : ''}`}
                    onClick={() => onSelect(item.query)}
                    onMouseEnter={() => {}}
                >
                    <span className="query-text">{item.query}</span>
                    <span className="query-count">{item.count}</span>
                </li>
            ))}
        </ul>
    );
};

export default SuggestionDropdown;
