import Dashboard from './dashboard/module';
import User from './user/module';
import File from './content/file/module';
import Attachment from './content/attachment/module';
import Subject from './content/subject/module';
import Plugin from './system/plugin/module';
import Setting from './system/setting/module';
import Tasks from './system/task/module';

const coreModules = [
	Dashboard,
	User,
	Attachment,
	File,
	Subject,
	Plugin,
	Setting,
	Tasks,
];

export { coreModules };
