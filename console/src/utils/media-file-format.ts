import { axiosInstance } from '@/utils/api-client';
import { getPostfix } from '@/utils/file';

export type MediaFileCategory =
	| 'VIDEO'
	| 'AUDIO'
	| 'IMAGE'
	| 'SUBTITLE'
	| 'LYRICS';

export interface MediaFileFormatHint {
	readonly format: string;
	readonly category: MediaFileCategory;
	readonly mimeType: string;
	readonly extensions: readonly string[];
}

export interface MediaFileFormatLookup {
	readonly accepts: readonly string[];
	categoryOf(fileName: string): MediaFileCategory | undefined;
	formatOf(fileName: string): string | undefined;
	mimeTypeOf(
		fileName: string,
		category?: MediaFileCategory
	): string | undefined;
}

let formatHintsRequest: Promise<readonly MediaFileFormatHint[]> | undefined;
let formatLookupRequest: Promise<MediaFileFormatLookup> | undefined;

const loadMediaFileFormatHints = () => {
	if (!formatHintsRequest) {
		formatHintsRequest = axiosInstance
			.get<readonly MediaFileFormatHint[]>('/api/v1/attachment/media-formats')
			.then((response) => {
				if (!Array.isArray(response?.data)) {
					throw new TypeError('Invalid media format hints response');
				}
				return response.data;
			})
			.catch((error) => {
				formatHintsRequest = undefined;
				throw error;
			});
	}
	return formatHintsRequest;
};

const uniqueValue = <T>(values: T[]): T | undefined => {
	const uniqueValues = new Set(values);
	return uniqueValues.size === 1
		? uniqueValues.values().next().value
		: undefined;
};

const createMediaFileFormatLookup = (
	hints: readonly MediaFileFormatHint[]
): MediaFileFormatLookup => {
	const hintsByExtension = new Map<string, MediaFileFormatHint[]>();
	for (const hint of hints) {
		for (const extension of hint.extensions) {
			const normalizedExtension = extension.toLowerCase();
			const extensionHints = hintsByExtension.get(normalizedExtension) || [];
			extensionHints.push(hint);
			hintsByExtension.set(normalizedExtension, extensionHints);
		}
	}

	const hintsOf = (fileName: string, category?: MediaFileCategory) => {
		const extension = getPostfix(fileName).toLowerCase();
		const extensionHints = hintsByExtension.get(extension) || [];
		return category
			? extensionHints.filter((hint) => hint.category === category)
			: extensionHints;
	};

	return {
		accepts: Array.from(hintsByExtension.keys())
			.sort()
			.map((extension) => `.${extension}`),
		categoryOf: (fileName) =>
			uniqueValue(hintsOf(fileName).map((hint) => hint.category)),
		formatOf: (fileName) =>
			uniqueValue(hintsOf(fileName).map((hint) => hint.format)),
		mimeTypeOf: (fileName, category) =>
			uniqueValue(hintsOf(fileName, category).map((hint) => hint.mimeType)),
	};
};

export const loadMediaFileFormatLookup = () => {
	if (!formatLookupRequest) {
		formatLookupRequest = loadMediaFileFormatHints()
			.then(createMediaFileFormatLookup)
			.catch((error) => {
				formatLookupRequest = undefined;
				throw error;
			});
	}
	return formatLookupRequest;
};
