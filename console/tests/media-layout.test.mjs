import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import ts from "typescript";
import { computed, ref, shallowRef } from "vue";

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

const photo = (index) => ({ type: "PHOTO", url: `photo-${index}` });
const video = (index) => ({ type: "VIDEO", url: `video-${index}` });

function loadEditorLayout(medium) {
  const formState = ref({ spec: { content: { medium } } });
  const logic = loadLogic(
    "../src/components/MomentEdit.vue",
    { computed, formState },
    "singlePhoto",
    (code) => code.startsWith("const mediaList") || code.startsWith("const singlePhoto")
  );
  return { ...logic, formState };
}

test("only a single photo is displayed in its own shape", () => {
  const { singlePhoto, formState } = loadEditorLayout([photo(1)]);
  const medium = formState.value.spec.content.medium;

  assert.equal(singlePhoto.value, true);

  medium.push(photo(2));
  assert.equal(singlePhoto.value, false);

  medium.splice(0, 2, photo(1));
  assert.equal(singlePhoto.value, true);

  medium.splice(0, 1, video(1));
  assert.equal(singlePhoto.value, false);

  medium.splice(0, 1);
  assert.equal(singlePhoto.value, false);
});

test("the console preview applies the same single photo rule", () => {
  const mediums = ref([photo(1)]);
  const { singlePhoto } = loadLogic(
    "../src/components/MomentPreview.vue",
    { computed, mediums },
    "singlePhoto",
    (code) => code.startsWith("const singlePhoto")
  );

  assert.equal(singlePhoto.value, true);

  mediums.value = [photo(1), photo(2)];
  assert.equal(singlePhoto.value, false);

  mediums.value = [video(1)];
  assert.equal(singlePhoto.value, false);

  mediums.value = [];
  assert.equal(singlePhoto.value, false);
});

test("selecting a tag inserts it into the editor and refreshes the parsed tags", () => {
  const calls = [];
  const editor = {
    chain: () => ({
      focus: () => ({
        setTag: (tag) => ({
          run: () => calls.push(["setTag", tag]),
        }),
      }),
    }),
  };
  const { handleEditorReady, handleInsertTag } = loadLogic(
    "../src/components/MomentEdit.vue",
    {
      shallowRef,
      queryEditorTags: () => calls.push(["queryEditorTags"]),
    },
    "handleEditorReady, handleInsertTag",
    (code) =>
      code.startsWith("const momentEditor") ||
      code.startsWith("const handleEditorReady") ||
      code.startsWith("const handleInsertTag")
  );

  handleInsertTag("旅行");
  assert.deepEqual(calls, []);

  handleEditorReady(editor);
  handleInsertTag("旅行");
  assert.deepEqual(calls, [["setTag", "旅行"], ["queryEditorTags"]]);
});