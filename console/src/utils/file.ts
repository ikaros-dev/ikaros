export function getPostfix(name: string): string {
	if (!name) {
		return '';
	}
	const normalizedName = name.split(/[?#]/, 1)[0].split(/[\\/]/).pop() || '';
	const dotIndex = normalizedName.lastIndexOf('.');
	return dotIndex > 0 && dotIndex < normalizedName.length - 1
		? normalizedName.substring(dotIndex + 1)
		: '';
}
