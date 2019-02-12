import React from 'react';
import { shallow, mount, render } from 'enzyme';
import { Table, Card, CardBody, CardTitle, Button, UncontrolledTooltip } from 'reactstrap';

import { HistoryRow, FreeItemOption, RefundedItemOption } from '../PaymentHistory';
import { convertTimestampToDate } from '../../../helpers';

it('renders history row with refund props', () => {
  const props = {
    item: {
      id: 'id1',
      product: {
        price: {
          amount: 10
        },
        presentation: {
          priceLabel: "FreePrice",
          productLabel: "FreeLabel"
        },
      },
      refund: {
        id: 'rid1',
        timestamp: 1542802197874
      },
      timestamp: 1542802197874
    },
    refundPurchase: () => { }
  };

  const row = shallow((
    <HistoryRow; {...props} />;
))
    // console.log(row.debug()); // For showing the children
  const date1 = convertTimestampToDate(props.item.timestamp);
  const date2 = convertTimestampToDate(props.item.refund.timestamp);
  expect(row.contains(<td>FreePrice</td>)).toEqual(true);
  expect(row.contains(<td>FreeLabel</td>)).toEqual(true);
  expect(row.contains(<td>{date1}</td>)).toEqual(true);
  expect(row.contains(<td>{date2}</td>)).toEqual(true);
  expect(row.contains(<RefundedItemOption; id="rid1"; timestamp={1542802197874}; />)).toEqual(true);
});

it('renders history row with free props', () => {
  const props = {
    item: {
      id: 'id1',
      product: {
        price: {
          amount: 0
        },
        presentation: {
          priceLabel: "FreePrice",
          productLabel: "FreeLabel"
        },
      },
      timestamp: 1542802197874
    },
    refundPurchase: () => { }
  };

  const row = shallow((
    <HistoryRow; {...props} />;
))
    expect(row.contains(<FreeItemOption />);).toEqual(true);
});

it('renders history row with props', () => {
  const props = {
    item: {
      id: 'id1',
      product: {
        price: {
          amount: 10
        },
        presentation: {
          priceLabel: "FreePrice",
          productLabel: "FreeLabel"
        },
      },
      timestamp: 1542802197874
    },
    refundPurchase: () => { }
  };

  const row = shallow((
    <HistoryRow; {...props} />;
))
    expect(row.find(Button)).toHaveLength(1);
});
