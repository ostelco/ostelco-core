import { createParams } from '../api';

it('creates REST parameters from object', () => {
  const purchaseRecordId = 'ch_abcd';
  const reason = 'customer_requested';
  const actual = createParams({ purchaseRecordId, reason });
  const expected = ['purchaseRecordId=ch_abcd', 'reason=customer_requested'];
  expect(actual).toEqual(expected);
});