import axios from 'axios';

const axiosInstance = axios.create({
  baseURL: 'http://localhost:8083/api/v1',
  withCredentials: true, // This allows the browser to send cookies automatically
});

// Interceptor to attach the Access Token to every request
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor to handle Access Token expiration
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If we get a 401, it means the Access Token is expired
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Attempt to get a new Access Token using the HttpOnly cookie
        const { data } = await axios.post(
          'http://localhost:8083/api/v1/auth/refresh', 
          {}, 
          { withCredentials: true } 
        );

        // Save the new token and retry the original request
        localStorage.setItem('accessToken', data.accessToken);
        
        axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${data.accessToken}`;
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        
        return axiosInstance(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('accessToken');
        
        // Only redirect if NOT already on the login page
        if (!window.location.pathname.includes('/login')) {
            window.location.href = '/login';
        }
        
        return Promise.reject(refreshError);
    }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;