"""Run against a local inventory API using python3 test_inventory.py."""

import json
import os
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from uuid import uuid4


BASE_URL = os.environ.get("INVENTORY_BASE_URL", "http://localhost:8081/api/v1")


class InventoryIntegrationTest(unittest.TestCase):
    def call(self, method, path, status=200, body=None):
        payload = None if body is None else json.dumps(body).encode()
        request = Request(BASE_URL + path, payload, {"Content-Type": "application/json"},
                          method=method)
        try:
            response = urlopen(request, timeout=10)
        except HTTPError as error:
            response = error
        with response:
            content = response.read().decode()
            self.assertEqual(response.status, status, f"{method} {path}: {content}")
            return json.loads(content)

    def stock(self, quantity):
        product = str(uuid4())
        self.call("POST", "/stocks", 201, {"productId": product, "quantity": quantity})
        return product

    def assert_stock(self, product, available, reserved):
        self.assertEqual(self.call("GET", f"/stocks/{product}"), {
            "productId": product, "available": available, "reserved": reserved,
        })

    @staticmethod
    def order():
        return f"/reservations/{uuid4()}"

    @staticmethod
    def lines(*products):
        return {"lines": [{"productId": product, "quantity": count} for product, count in products]}

    def test_failed_second_line_rolls_back_first_line_and_reservation(self):
        first, second = sorted((self.stock(5), self.stock(5)))
        order = self.order()
        error = self.call("PUT", order, 409, self.lines((first, 2), (second, 6)))
        self.assertEqual(error["code"], "INSUFFICIENT_STOCK")
        self.assert_stock(first, 5, 0)
        self.assert_stock(second, 5, 0)
        self.call("GET", order, 404)
        self.call("PUT", order, body=self.lines((first, 2), (second, 1)))
        self.assert_stock(first, 3, 2)
        self.assert_stock(second, 4, 1)

    def test_reservation_and_release_retries_change_stock_once(self):
        first, second = self.stock(5), self.stock(8)
        order = self.order()
        reservation = self.call("PUT", order, body=self.lines((first, 2), (second, 3)))
        self.assertEqual(reservation["status"], "RESERVED")
        self.assertEqual(self.call("PUT", order, body=self.lines((second, 3), (first, 2))),
                         reservation)
        self.call("PUT", order, 409, self.lines((first, 1), (second, 3)))
        self.assert_stock(first, 3, 2)
        self.assert_stock(second, 5, 3)
        for _ in range(2):
            self.assertEqual(self.call("PUT", order + "/release")["status"], "RELEASED")
        self.call("PUT", order + "/confirm", 409)
        self.assert_stock(first, 5, 0)
        self.assert_stock(second, 8, 0)

    def test_confirmation_retry_consumes_reserved_stock_once(self):
        product = self.stock(10)
        order = self.order()
        self.call("PUT", order, body=self.lines((product, 4)))
        self.call("POST", f"/stocks/{product}/adjustments", 409, {"delta": -7})
        self.assert_stock(product, 6, 4)
        for _ in range(2):
            self.assertEqual(self.call("PUT", order + "/confirm")["status"], "CONFIRMED")
        self.call("PUT", order + "/release", 409)
        self.assert_stock(product, 6, 0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
