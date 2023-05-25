import axios from "axios";
import type { AxiosInstance } from "axios";

const baseURL = import.meta.env.VITE_API_URL;

const axiosInstance = axios.create({
  baseURL,
  withCredentials: true,
});


axiosInstance.interceptors.response.use(
    (response) => {
      return response;
    }
    // todo need global exception handle
  );
  
axiosInstance.defaults.headers.common["X-Requested-With"] = "XMLHttpRequest";


function setupApiClient(axios: AxiosInstance) {
    return {
        // todo need add api client types
    }
}

const apiClient = setupApiClient(axiosInstance);
  
export { apiClient };
