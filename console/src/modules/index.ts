import Dashboard from './dashboard/module';
import User from './user/module';
import Attachment from './content/attachment/module';
import Subject from './content/subject/module';
import Plugin from './system/plugin/module';
import Setting from './system/setting/module';
import About from './system/about/module';
import Tasks from './system/task/module';
import Users from './system/user/module';
import Roles from './system/role/module';

const coreModules = [
	Dashboard,
	User,
	Attachment,
	Subject,
	Plugin,
	Setting,
    About,
    Tasks,
	Users,
	Roles,
];

export { coreModules };
