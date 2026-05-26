import axios from 'axios';

const axiosInstance = axios.create({
    baseURL: 'http://localhost:8083/api/v1', // Ensure this matches your backend port
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // Crucial for sending/receiving cookies (refreshToken)
});

export default axiosInstance;