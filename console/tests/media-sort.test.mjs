import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import ts from "typescript";
import { ref } from "vue";

// Execute the actual SFC logic without loading the editor, host APIs, or a DOM.
function loadLogic(file, bindings, names, select = () => true) {
  const source = readFileSync(new URL(file, import.meta.url), "utf8").match(
    /<script[^>]*>([\s\S]*?)<\/script>/
  )[1];
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true);
  const code = ast.statements
    .filter((node) => !ts.isImportDeclaration(node) && select(node.getText(ast)))
    .map((node) => node.getText(ast))
    .join("\n");
  const js = ts.transpile(code, { target: ts.ScriptTarget.ES2022 });
  return new Function(...Object.keys(bindings), `${js}; return { ${names} };`)(
    ...Object.values(bindings)
  );
}

function createEditor(count = 9) {
  const medium = Array.from({ length: count }, (_, index) => ({
    type: "PHOTO",
    url: `photo-${index + 1}`,
  }));
  const formState = ref({ spec: { content: { medium } } });
  const logic = loadLogic(
    "../src/components/MomentEdit.vue",
    { ref, formState },
    "moveMedium, toggleMediaSorting, sortingMediaIndex, mediaDirections",
    (code) =>
      code.startsWith("const MEDIA_GRID_COLUMNS") ||
      code.startsWith("const sortingMediaIndex") ||
      code.startsWith("const toggleMediaSorting") ||
      code.startsWith("const mediaTargetIndex") ||
      code.startsWith("const moveMedium") ||
      code.startsWith("const mediaDirections")
  );
  return { logic, medium };
}

// Only the two swapped positions may change, which keeps the rest of the grid stable.
function assertOnlySwapped(medium, before, first, second) {
  medium.forEach((media, index) => {
    if (index === first) {
      assert.equal(media.url, before[second]);
    } else if (index === second) {
      assert.equal(media.url, before[first]);
    } else {
      assert.equal(media.url, before[index]);
    }
  });
}

test("images are swapped with the neighbour in the requested direction", () => {
  const { logic, medium } = createEditor();
  let before = medium.map((media) => media.url);

  logic.moveMedium(0, "right");
  assertOnlySwapped(medium, before, 0, 1);

  before = medium.map((media) => media.url);
  logic.moveMedium(4, "left");
  assertOnlySwapped(medium, before, 4, 3);

  before = medium.map((media) => media.url);
  logic.moveMedium(4, "up");
  assertOnlySwapped(medium, before, 4, 1);

  before = medium.map((media) => media.url);
  logic.moveMedium(1, "down");
  assertOnlySwapped(medium, before, 1, 4);
});

test("images cannot leave the grid boundaries", () => {
  const { logic, medium } = createEditor();
  const original = medium.map((media) => media.url).join(",");

  logic.moveMedium(0, "up");
  logic.moveMedium(0, "left");
  logic.moveMedium(2, "right");
  logic.moveMedium(6, "down");
  logic.moveMedium(8, "right");
  logic.moveMedium(8, "down");

  assert.equal(medium.map((media) => media.url).join(","), original);
  assert.deepEqual(logic.mediaDirections(0), ["down", "right"]);
  assert.deepEqual(logic.mediaDirections(2), ["down", "left"]);
  assert.deepEqual(logic.mediaDirections(8), ["up", "left"]);
});

test("the sorting overlay follows the moved image and can be toggled", () => {
  const { logic } = createEditor();

  logic.toggleMediaSorting(4);
  assert.equal(logic.sortingMediaIndex.value, 4);

  logic.moveMedium(4, "up");
  assert.equal(logic.sortingMediaIndex.value, 1);

  logic.toggleMediaSorting(1);
  assert.equal(logic.sortingMediaIndex.value, null);

  logic.toggleMediaSorting(5);
  assert.equal(logic.sortingMediaIndex.value, 5);
  logic.toggleMediaSorting(6);
  assert.equal(logic.sortingMediaIndex.value, 6);
});

test("a single image has nothing to swap with", () => {
  const { logic } = createEditor(1);

  assert.deepEqual(logic.mediaDirections(0), []);
});