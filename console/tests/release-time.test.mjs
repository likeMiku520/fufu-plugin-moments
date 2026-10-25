import { fromDate, getLocalTimeZone, toCalendarDateTime } from "@internationalized/date";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import ts from "typescript";
import { computed, effectScope, nextTick, ref, shallowRef, watch } from "vue";

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

test("UC only sends an explicitly changed release time", async () => {
  for (const changed of [false, true]) {
    const selected = new Date("2026-08-01T00:00:00Z");
    let saved;
    const { handleUpdate } = loadLogic(
      "../src/uc/MomentEdit.vue",
      {
        releaseTimeChanged: ref(changed),
        releaseTime: ref(selected),
        momentsUcApiClient: {
          moment: {
            getMyMoment: async () => ({
              data: {
                metadata: { name: "test", version: 2 },
                spec: { releaseTime: "2026-09-01T00:00:00Z" },
              },
            }),
            updateMyMoment: async ({ moment }) => {
              saved = JSON.parse(JSON.stringify(moment));
            },
          },
        },
        emit() {},
        queryClient: { invalidateQueries() {} },
        Toast: { success() {} },
      },
      "handleUpdate",
      (code) => code.startsWith("const handleUpdate =")
    );
    await handleUpdate({
      metadata: { name: "test", version: 1 },
      spec: {
        releaseTime: selected.toISOString(),
        content: { raw: "edited" },
      },
    });
    assert.equal(saved.metadata.version, 2);
    assert.equal(saved.spec.releaseTime, changed ? selected.toISOString() : undefined);
    assert.equal(saved.spec.content.raw, "edited");
  }
});

test("picker advances its reference time while open and stops when closed", async () => {
  const scope = effectScope();
  let refresh,
    active = false;
  const model = ref(null);
  try {
    const picker = scope.run(() =>
      loadLogic(
        "../src/components/ReleaseTimePicker.vue",
        {
          defineModel: () => model,
          computed,
          ref,
          shallowRef,
          watch,
          fromDate,
          getLocalTimeZone,
          toCalendarDateTime,
          useIntervalFn: (callback) => {
            refresh = callback;
            return {
              resume: () => {
                active = true;
              },
              pause: () => {
                active = false;
              },
            };
          },
        },
        "handleOpen, open, draft, current, valid, draftScheduled, scheduled, apply"
      )
    );
    picker.handleOpen(true);
    await nextTick();
    assert.equal(active, true);
    // A popup left open for two minutes must treat a one-minute-ago time as past.
    picker.current.value = toCalendarDateTime(
      fromDate(new Date(Date.now() - 120000), getLocalTimeZone())
    );
    picker.draft.value = toCalendarDateTime(
      fromDate(new Date(Date.now() - 60000), getLocalTimeZone())
    );
    assert.equal(picker.draftScheduled.value, true);
    refresh();
    assert.equal(picker.draftScheduled.value, false);
    assert.equal(picker.valid.value, true);
    // A time in the future is accepted and marks the moment as scheduled.
    picker.draft.value = toCalendarDateTime(
      fromDate(new Date(Date.now() + 3600000), getLocalTimeZone())
    );
    assert.equal(picker.draftScheduled.value, true);
    picker.apply();
    assert.notEqual(model.value, null);
    assert.equal(picker.scheduled.value, true);
    picker.open.value = false;
    await nextTick();
    assert.equal(active, false);
  } finally {
    scope.stop();
  }
});
