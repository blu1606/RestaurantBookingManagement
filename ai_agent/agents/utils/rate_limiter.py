import time
from typing import Dict, Optional
from datetime import datetime, timedelta

class RateLimiter:
    """
    Simple rate limiter to handle API quota issues
    """
    
    def __init__(self, max_requests: int = 8, time_window: int = 60):
        """
        Initialize rate limiter
        
        Args:
            max_requests: Maximum requests allowed in time window
            time_window: Time window in seconds
        """
        self.max_requests = max_requests
        self.time_window = time_window
        self.requests = []
    
    def can_make_request(self) -> bool:
        """
        Check if a request can be made
        """
        now = datetime.now()
        
        # Remove old requests outside the time window
        self.requests = [req_time for req_time in self.requests 
                        if now - req_time < timedelta(seconds=self.time_window)]
        
        # Check if we can make a new request
        if len(self.requests) < self.max_requests:
            return True
        
        return False
    
    def record_request(self):
        """
        Record a request
        """
        self.requests.append(datetime.now())
    
    def get_wait_time(self) -> int:
        """
        Get time to wait before next request can be made
        """
        if not self.requests:
            return 0
        
        now = datetime.now()
        oldest_request = min(self.requests)
        time_since_oldest = (now - oldest_request).total_seconds()
        
        if time_since_oldest >= self.time_window:
            return 0
        
        return int(self.time_window - time_since_oldest)
    
    def wait_if_needed(self):
        """
        Wait if rate limit is exceeded
        """
        if not self.can_make_request():
            wait_time = self.get_wait_time()
            if wait_time > 0:
                print(f"⏳ Rate limit exceeded. Waiting {wait_time} seconds...")
                time.sleep(wait_time)
        
        self.record_request() 