import Dashboard from './dashboard/module';
import User from './system/user/module';
import File from './content/file/module';
import Subject from './content/subject/module';
import Plugin from './system/plugin/module';

const coreModules = [Dashboard, User, File, Subject, Plugin];

export { coreModules };
