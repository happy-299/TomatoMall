import { axios } from '../utils/request'
import { ADVERTISEMENT_MODULE } from './_prefix'

export interface Advertisement {
    id: string
    title: string
    content: string
    imgUrl: string
    productId: string
}

export interface UpdateAdvertisementPayload {
    id: string;
    productId: string;
    title?: string;
    content?: string;
    imgUrl?: string;
}

export const updateAdvertisement = (data: UpdateAdvertisementPayload) => {
    const token = sessionStorage.getItem('token');
    return axios.put(ADVERTISEMENT_MODULE, data, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    });
};

export const getAdvertisements = () => {
    return axios.get(ADVERTISEMENT_MODULE)
}

export const createAdvertisement = (data: {
    title: string
    content: string
    imgUrl: string
    productId: string
}) => {
    const token = sessionStorage.getItem('token')
    return axios.post(ADVERTISEMENT_MODULE, data, {
        headers: {
            'Content-Type': 'application/json',
            'token': token || ''
        }
    })
}

export const deleteAdvertisement = (id: string) => {
    const token = sessionStorage.getItem('token')
    return axios.delete(`${ADVERTISEMENT_MODULE}/${id}`, {
        headers: {
            'token': token || ''
        }
    })
}