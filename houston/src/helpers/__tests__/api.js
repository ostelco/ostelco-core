import { createParams, transformError } from '../api';

it('creates REST parameters from object', () => {
  const purchaseRecordId = 'ch_abcd';
  const reason = 'customer_requested';
  const expected =  '?purchaseRecordId=ch_abcd&reason=customer_requested';
  const actual = createParams({ purchaseRecordId, reason });
  expect(actual).toEqual(expected);
});

it('creates REST parameters from undefined', () => {
  const expected =  '';
  const actual = createParams();
  expect(actual).toEqual(expected);
});

it('creates REST parameters from null', () => {
  const expected =  '';
  const actual = createParams();
  expect(actual).toEqual(expected);
});

it('creates REST parameters from empty object', () => {
  const expected =  '';
  const actual = createParams({});
  expect(actual).toEqual(expected);
});

it('transform Error object with array of errors', () => {
  const errorObj = { errors: ['fail 1', 'fail 2'] };
  const expected =  'fail 1, fail 2';
  const actual = transformError(errorObj);
  expect(actual).toEqual(expected);
});

it('transform Error object with  errors', () => {
  const errorObj = { errors: 'fail 1' };
  const expected =  'fail 1';
  const actual = transformError(errorObj);
  expect(actual).toEqual(expected);
});

it('transform Error object with message property', () => {
  const errorObj = { message: 'fail 1' };
  const expected =  'fail 1';
  const actual = transformError(errorObj);
  expect(actual).toEqual(expected);
});

it('transform Error object with error property', () => {
  const errorObj = { error: 'fail 1' };
  const expected =  'fail 1';
  const actual = transformError(errorObj);
  expect(actual).toEqual(expected);
});

it('transform Error object without any properties', () => {
  const errorObj = "fail 1";
  const expected =  'fail 1';
  const actual = transformError(errorObj);
  expect(actual).toEqual(expected);
});

it('transform Error object without any properties', () => {
  const errorObj = 1;
  const expected =  'Something bad happened';
  const actual = transformError(errorObj);
  expect(actual).toEqual(expected);
});
