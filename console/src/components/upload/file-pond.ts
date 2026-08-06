import { FileStatus } from 'filepond';
import { getPostfix } from '@/utils/file';

const ACTIVE_FILE_STATUSES = new Set([
	FileStatus.LOADING,
	FileStatus.PROCESSING_QUEUED,
	FileStatus.PROCESSING,
]);

export const detectFileExtensionType = (file: Pick<File, 'name'>) => {
	const extension = getPostfix(file.name).toLowerCase();
	return Promise.resolve(extension ? `.${extension}` : '');
};

export const hasActiveFileTransfers = (
	files: readonly { readonly status: FileStatus }[]
) => files.some((file) => ACTIVE_FILE_STATUSES.has(file.status));
