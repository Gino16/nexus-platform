"""Run with the catalog, inventory, and order APIs connected to local databases."""

import json
import os
import time
import unittest
from concurrent.futures import ThreadPoolExecutor
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from uuid import uuid4


CATALOG = os.getenv("CATALOG_URL", "http://localhost:8080/api/v1")
INVENTORY = os.getenv("INVENTORY_URL", "http://localhost:8081/api/v1")
ORDERS = os.getenv("ORDERS_URL", "http://localhost:8082/api/v1")


class CheckoutIntegrationTest(unittest.TestCase):
    def call(self, base, method, path, status=200, body=None):
        payload = json.dumps(body).encode() if body is not None else None
        request = Request(base + path, payload, {"Content-Type": "application/json"}, method=method)
        try:
            response = urlopen(request, timeout=10)
        except HTTPError as error:
            response = error
        with response:
            content = response.read().decode()
            self.assertEqual(response.status, status, f"{method} {path}: {content}")
            return json.loads(content)

    def stock(self, quantity, price):
        product = self.call(CATALOG, "POST", "/products", 201,
                            {"name": f"Compra {uuid4()}", "description": "Prueba", "price": price})
        self.call(INVENTORY, "POST", "/stocks", 201,
                  {"productId": product["id"], "quantity": quantity})
        return product["id"]

    def submit(self, outcome, *items):
        path = f"/orders/{uuid4()}"
        request = {"customerId": str(uuid4()), "paymentOutcome": outcome,
                   "lines": [{"productId": product, "quantity": qty} for product, qty in items]}
        self.call(ORDERS, "PUT", path, 202, request)
        return path, request

    def terminal(self, path):
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            order = self.call(ORDERS, "GET", path)
            if order["status"] in ("CONFIRMED", "REJECTED"):
                return order
            time.sleep(0.1)
        self.fail(f"{path} did not reach a terminal state")

    def assert_stock(self, product, available):
        stock = self.call(INVENTORY, "GET", f"/stocks/{product}")
        self.assertEqual((stock["available"], stock["reserved"]), (available, 0))

    def test_purchase_confirms_and_preserves_price_snapshot(self):
        first, second = self.stock(10, 12.35), self.stock(5, 2.50)
        path, request = self.submit("APPROVED", (first, 2), (second, 3))
        self.call(CATALOG, "PUT", f"/products/{first}", 200,
                  {"name": "Precio cambiado", "description": "", "price": 90.00, "active": True})
        confirmed = self.terminal(path)
        self.assertEqual(confirmed["status"], "CONFIRMED")
        self.assertEqual(confirmed["total"], 32.20)
        self.assertEqual({line["productId"]: line["unitPrice"] for line in confirmed["lines"]},
                         {first: 12.35, second: 2.50})
        request["lines"].reverse()
        self.assertEqual(self.call(ORDERS, "PUT", path, 202, request), confirmed)
        request["lines"][0]["quantity"] += 1
        self.call(ORDERS, "PUT", path, 409, request)
        self.assert_stock(first, 8)
        self.assert_stock(second, 2)

    def test_declined_payment_releases_both_products(self):
        first, second = self.stock(10, 8), self.stock(5, 2)
        path, _ = self.submit("DECLINED", (first, 2), (second, 3))
        order = self.terminal(path)
        self.assertEqual((order["status"], order["rejectionReason"]), ("REJECTED", "PAYMENT_DECLINED"))
        reservation = self.call(INVENTORY, "GET", f"/reservations/{order['orderId']}")
        self.assertEqual(reservation["status"], "RELEASED")
        self.assert_stock(first, 10)
        self.assert_stock(second, 5)

    def test_insufficient_stock_rejects_without_partial_reservation(self):
        first, second = sorted((self.stock(4, 1), self.stock(4, 2)))
        path, _ = self.submit("APPROVED", (first, 1), (second, 5))
        order = self.terminal(path)
        self.assertEqual((order["status"], order["rejectionReason"]),
                         ("REJECTED", "INVENTORY_UNAVAILABLE"))
        self.call(INVENTORY, "GET", f"/reservations/{order['orderId']}", 404)
        self.assert_stock(first, 4)
        self.assert_stock(second, 4)

    def test_simultaneous_orders_cannot_oversell(self):
        product = self.stock(1, 1)
        with ThreadPoolExecutor(max_workers=2) as pool:
            orders = list(pool.map(lambda _: self.submit("APPROVED", (product, 1)), range(2)))
        self.assertCountEqual([self.terminal(path)["status"] for path, _ in orders],
                              ["CONFIRMED", "REJECTED"])
        self.assert_stock(product, 0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
