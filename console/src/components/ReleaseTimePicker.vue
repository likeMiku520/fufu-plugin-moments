<script setup lang="ts">
import "@/styles/date-picker.scss";
import { VButton } from "@halo-dev/components";
import {
  fromDate,
  getLocalTimeZone,
  toCalendarDateTime,
  type DateValue,
} from "@internationalized/date";
import { useIntervalFn } from "@vueuse/core";
import {
  DatePickerCalendar,
  DatePickerCell,
  DatePickerCellTrigger,
  DatePickerContent,
  DatePickerField,
  DatePickerGrid,
  DatePickerGridBody,
  DatePickerGridHead,
  DatePickerGridRow,
  DatePickerHeadCell,
  DatePickerHeader,
  DatePickerHeading,
  DatePickerInput,
  DatePickerNext,
  DatePickerPrev,
  DatePickerRoot,
  DatePickerTrigger,
} from "reka-ui";
import { computed, ref, shallowRef, watch } from "vue";
import ChevronLeft from "~icons/tabler/chevron-left";
import ChevronRight from "~icons/tabler/chevron-right";
import Clock from "~icons/tabler/clock";

const model = defineModel<Date | null>({ required: true });
const zone = getLocalTimeZone();
const open = ref(false);
const draft = shallowRef<DateValue>();
const current = shallowRef(toCalendarDateTime(fromDate(new Date(), zone)));
const { pause, resume } = useIntervalFn(
  () => {
    current.value = toCalendarDateTime(fromDate(new Date(), zone));
  },
  1000,
  { immediate: false }
);
watch(open, (value) => (value ? resume() : pause()));
const valid = computed(() => !!draft.value);
const draftScheduled = computed(() => !!draft.value && draft.value.compare(current.value) > 0);
const scheduled = computed(() => !!model.value && model.value.getTime() > Date.now());
function format(value: DateValue) {
  const date = toCalendarDateTime(value);
  return `${date.toString().slice(0, 10)} ${String(date.hour).padStart(2, "0")}:${String(date.minute).padStart(2, "0")}`;
}
const label = computed(() => {
  if (!model.value) return "发布时间，默认为当前时间";
  const text = format(fromDate(model.value, zone));
  return scheduled.value ? `定时发布：${text}` : text;
});
const draftLabel = computed(() => (draft.value ? format(draft.value) : ""));
function handleOpen(value: boolean) {
  if (value) {
    current.value = toCalendarDateTime(fromDate(new Date(), zone));
    draft.value = model.value ? toCalendarDateTime(fromDate(model.value, zone)) : current.value;
  }
  open.value = value;
}
function apply() {
  if (!valid.value || !draft.value) return;
  model.value = draft.value.toDate(zone);
  open.value = false;
}
function reset() {
  model.value = new Date();
  open.value = false;
}
</script>
<template>
  <DatePickerRoot
    v-model="draft"
    :open="open"
    locale="zh-CN"
    granularity="minute"
    :hour-cycle="24"
    prevent-deselect
    @update:open="handleOpen"
  >
    <DatePickerTrigger as-child>
      <button
        type="button"
        :aria-label="label"
        :title="label"
        class=":uno: group inline-flex cursor-pointer items-center justify-center gap-1.5 rounded-full p-2 text-gray-600 hover:bg-sky-600/10 hover:text-sky-600 focus-visible:outline-sky-600"
      >
        <Clock class=":uno: size-4 shrink-0" />
        <span v-if="model" class=":uno: whitespace-nowrap text-xs tabular-nums">{{ label }}</span>
      </button>
    </DatePickerTrigger>
    <DatePickerContent
      class=":uno: moments-date-popover"
      align="end"
      :side-offset="6"
      :collision-padding="8"
      aria-label="选择发布时间"
    >
      <DatePickerCalendar v-slot="{ grid, weekDays }" calendar-label="发布时间">
        <DatePickerHeader class=":uno: moments-calendar-header">
          <DatePickerPrev class=":uno: moments-calendar-nav" aria-label="上个月"
            ><ChevronLeft
          /></DatePickerPrev>
          <DatePickerHeading />
          <DatePickerNext class=":uno: moments-calendar-nav" aria-label="下个月"
            ><ChevronRight
          /></DatePickerNext>
        </DatePickerHeader>
        <div class=":uno: moments-calendar-months">
          <DatePickerGrid
            v-for="month in grid"
            :key="month.value.toString()"
            class=":uno: moments-calendar-grid"
          >
            <DatePickerGridHead>
              <DatePickerGridRow>
                <DatePickerHeadCell
                  v-for="day in weekDays"
                  :key="day"
                  class=":uno: moments-calendar-weekday"
                  >{{ day }}</DatePickerHeadCell
                >
              </DatePickerGridRow>
            </DatePickerGridHead>
            <DatePickerGridBody>
              <DatePickerGridRow v-for="(week, index) in month.rows" :key="index">
                <DatePickerCell
                  v-for="day in week"
                  :key="day.toString()"
                  :date="day"
                  class=":uno: moments-calendar-cell"
                >
                  <DatePickerCellTrigger
                    :day="day"
                    :month="month.value"
                    class=":uno: moments-calendar-day moments-calendar-single-day"
                  />
                </DatePickerCell>
              </DatePickerGridRow>
            </DatePickerGridBody>
          </DatePickerGrid>
        </div>
      </DatePickerCalendar>
      <div class=":uno: moments-date-field-row">
        <span class=":uno: text-xs text-gray-600">发布时间</span>
        <DatePickerField
          v-slot="{ segments }"
          class=":uno: moments-date-field"
          aria-label="发布时间"
        >
          <DatePickerInput
            v-for="segment in segments"
            :key="segment.part"
            :part="segment.part"
            class=":uno: moments-date-segment"
            >{{ segment.value }}</DatePickerInput
          >
        </DatePickerField>
      </div>
      <p v-if="!valid" class=":uno: mt-2 text-xs text-red-600" role="alert">
        请选择完整的日期和时间
      </p>
      <p v-else-if="draftScheduled" class=":uno: mt-2 text-xs text-gray-500">
        定时发布：将在 {{ draftLabel }} 自动发布
      </p>
      <div class=":uno: moments-date-actions">
        <button type="button" class=":uno: moments-date-text-button" @click="reset">
          恢复当前时间
        </button>
        <div class=":uno: flex gap-2">
          <VButton size="sm" @click="open = false">取消</VButton>
          <VButton size="sm" type="secondary" :disabled="!valid" @click="apply">确定</VButton>
        </div>
      </div>
    </DatePickerContent>
  </DatePickerRoot>
</template>
