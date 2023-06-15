/**
 * Format date.
 *
 * example:
 * const date = new Date();
 * const format = 'yyyy-MM-dd HH:mm:ss';
 * const formattedDate = formatDate(date, format);
 * console.log(formattedDate); // 2022-09-08 14:30:00
 *
 * @param date  date
 * @param format format str
 * @returns formatted str
 */
export const formatDate = (
	date: Date | String,
	format = 'yyyy-MM-dd HH:mm:ss'
): string => {
	// console.log('date', date)
	// console.log('typeof date', typeof date)
	// console.log('!(date instanceof Date)', !(date instanceof Date))
	if (!(date instanceof Date)) {
		date = new Date(date as string);
	}
	const year = date.getFullYear();
	const month = date.getMonth() + 1;
	const day = date.getDate();
	const hour = date.getHours();
	const minute = date.getMinutes();
	const second = date.getSeconds();
	const formatMap: { [key: string]: any } = {
		yyyy: year.toString(),
		MM: month.toString().padStart(2, '0'),
		dd: day.toString().padStart(2, '0'),
		HH: hour.toString().padStart(2, '0'),
		mm: minute.toString().padStart(2, '0'),
		ss: second.toString().padStart(2, '0'),
	};
	return format.replace(/yyyy|MM|dd|HH|mm|ss/g, (match) => formatMap[match]);
};
