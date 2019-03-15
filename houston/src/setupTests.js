import { configure } from 'enzyme';
import Adapter from 'enzyme-profilevendors-react-16';
import 'jest-enzyme';

configure({ adapter: new Adapter() });
