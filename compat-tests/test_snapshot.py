"""Prove canonical palette expansion does not hide terrain mismatches."""
import unittest

from smoke import expand_palette, first_difference, normalize


def pack(indices, bits):
    per_long = 64 // bits
    result = []
    for start in range(0, len(indices), per_long):
        word = sum(value << (offset * bits) for offset, value in enumerate(indices[start:start + per_long]))
        result.append(word if word < (1 << 63) else word - (1 << 64))
    return result


class SnapshotTest(unittest.TestCase):
    def test_equivalent_palette_reordering(self):
        a = {"palette": ["stone", "air"], "data": pack([0, 1] * 2048, 4)}
        b = {"palette": ["air", "stone"], "data": pack([1, 0] * 2048, 4)}
        self.assertEqual(expand_palette(a, 4096, 4), expand_palette(b, 4096, 4))

    def test_signed_words_preserve_high_bit(self):
        palette = list(range(16))
        expanded = expand_palette({"palette": palette, "data": [-1] * 256}, 4096, 4)
        self.assertEqual(expanded, [15] * 4096)

    def test_rejects_invalid_length_and_palette_index(self):
        with self.assertRaisesRegex(ValueError, "length"):
            expand_palette({"palette": ["a", "b"], "data": []}, 4096, 4)
        with self.assertRaisesRegex(ValueError, "out of bounds"):
            expand_palette({"palette": ["a", "b"], "data": [3] * 256}, 4096, 4)

    def test_keeps_generation_data_and_excludes_only_runtime_counters(self):
        a = normalize({"LastUpdate": 1, "InhabitedTime": 3, "Heightmaps": {"surface": [1]}, "fluid_ticks": [2]})
        b = normalize({"LastUpdate": 9, "InhabitedTime": 4, "Heightmaps": {"surface": [1]}, "fluid_ticks": [2]})
        self.assertIsNone(first_difference(a, b))
        b["fluid_ticks"][0] = 3
        self.assertEqual(first_difference(a, b), "$.fluid_ticks[0]")


if __name__ == "__main__":
    unittest.main()
