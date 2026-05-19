import moment from 'moment';

export const dateFormat = (row, column) => {
	const date = row[column.property];

	if (date == undefined) {
		return '';
	}

	return moment(date).format('YYYY-MM-DD HH:mm:ss');
};
