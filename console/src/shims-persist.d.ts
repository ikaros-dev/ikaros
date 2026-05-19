import 'pinia';

declare module 'pinia' {
	export interface DefineSetupStoreOptions<Id extends string, S extends StateTree, G, A> {
		persist?: {
			enabled: true;
			strategies?: Array<{
				key?: string;
				storage?: Storage;
				paths?: string[];
			}>;
		};
	}
}
