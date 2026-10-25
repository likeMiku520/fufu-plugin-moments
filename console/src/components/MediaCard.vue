<script lang="ts" setup>
import type { MomentMedia } from "@/api/generated";
import "@/styles/media-sort.scss";
import type { MediaMoveDirection } from "@/types";
import { utils } from "@halo-dev/ui-shared";
import LucideFileAudio from "~icons/lucide/file-audio";
import LucideFileVideo from "~icons/lucide/file-video";
import MingCloseCircle from "~icons/mingcute/close-circle-fill";
import MingEyeFill from "~icons/mingcute/eye-fill";
import TablerArrowDown from "~icons/tabler/arrow-down";
import TablerArrowLeft from "~icons/tabler/arrow-left";
import TablerArrowRight from "~icons/tabler/arrow-right";
import TablerArrowUp from "~icons/tabler/arrow-up";

const props = withDefaults(
  defineProps<{
    media: MomentMedia;
    sortable?: boolean;
    sorting?: boolean;
    directions?: MediaMoveDirection[];
    single?: boolean;
  }>(),
  {
    sortable: false,
    sorting: false,
    directions: () => [],
    single: false,
  }
);

const emit = defineEmits<{
  (event: "remove", media: MomentMedia): void;
  (event: "toggle-sorting"): void;
  (event: "move", direction: MediaMoveDirection): void;
}>();

const handleRemoveClick = () => {
  emit("remove", props.media);
};

const handleToggleSorting = () => {
  emit("toggle-sorting");
};

const handleMove = (direction: MediaMoveDirection) => {
  emit("move", direction);
};

const canMove = (direction: MediaMoveDirection) => props.directions.includes(direction);

const canPlayType = (type?: string) => {
  if (!type) {
    return false;
  }
  const obj = document.createElement("video");
  return !!obj.canPlayType(type);
};
const audioType = (type?: string) => {
  if (!type) {
    return false;
  }
  const obj = document.createElement("audio");
  return !!obj.canPlayType(type);
};

const getExtname = (type?: string) => {
  if (!type) {
    return "";
  }
  const ext = type.split("/");
  if (ext.length > 1) {
    if (ext) return ext[1].toLowerCase();
  }
  return "";
};
</script>
<template>
  <div class=":uno: relative overflow-hidden">
    <template v-if="media.type == 'PHOTO'">
      <div :class="single ? ':uno: w-full' : ':uno: aspect-square'">
        <img
          :src="utils.attachment.getThumbnailUrl(media.url!, single ? 'M' : 'S')"
          :class="single ? ':uno: block h-auto w-full' : ':uno: size-full object-cover'"
          loading="lazy"
        />
      </div>
    </template>
    <template v-else-if="media.type == 'VIDEO'">
      <div class=":uno: aspect-square">
        <video
          v-if="canPlayType(media.originType)"
          class=":uno: size-full object-cover"
          preload="metadata"
        >
          <source :src="media.url" :type="media.originType" />
        </video>
        <div
          v-else
          class=":uno: size-full flex flex-col items-center justify-center bg-gray-100 space-y-1"
        >
          <LucideFileVideo />
          <span class=":uno: text-xs text-gray-500 font-sans">
            {{ getExtname(media.originType) }}
          </span>
        </div>
      </div>
    </template>
    <template v-else-if="media.type == 'AUDIO'">
      <div class=":uno: aspect-square">
        <audio v-if="audioType(media.originType)" class=":uno: object-cover" preload="metadata">
          <source :src="media.url" :type="media.originType" />
        </audio>
        <div
          v-else
          class=":uno: size-full flex flex-col items-center justify-center bg-gray-100 space-y-1"
        >
          <LucideFileAudio />
          <span class=":uno: text-xs text-gray-500 font-sans">
            {{ getExtname(media.originType) }}
          </span>
        </div>
      </div>
    </template>

    <button
      v-if="sortable && directions.length > 0"
      type="button"
      class=":uno: moments-media-sort-toggle"
      :aria-label="sorting ? '结束调整顺序' : '调整图片顺序'"
      v-tooltip="sorting ? '结束调整顺序' : '调整图片顺序'"
      @click="handleToggleSorting"
    >
      <MingEyeFill />
    </button>

    <div v-if="sorting" class=":uno: moments-media-arrows">
      <button
        type="button"
        class=":uno: moments-media-arrow moments-media-arrow--up"
        aria-label="与上一行交换位置"
        v-tooltip="'与上一行交换位置'"
        :disabled="!canMove('up')"
        @click="handleMove('up')"
      >
        <TablerArrowUp />
      </button>
      <button
        type="button"
        class=":uno: moments-media-arrow moments-media-arrow--left"
        aria-label="与左侧交换位置"
        v-tooltip="'与左侧交换位置'"
        :disabled="!canMove('left')"
        @click="handleMove('left')"
      >
        <TablerArrowLeft />
      </button>
      <button
        type="button"
        class=":uno: moments-media-arrow moments-media-arrow--right"
        aria-label="与右侧交换位置"
        v-tooltip="'与右侧交换位置'"
        :disabled="!canMove('right')"
        @click="handleMove('right')"
      >
        <TablerArrowRight />
      </button>
      <button
        type="button"
        class=":uno: moments-media-arrow moments-media-arrow--down"
        aria-label="与下一行交换位置"
        v-tooltip="'与下一行交换位置'"
        :disabled="!canMove('down')"
        @click="handleMove('down')"
      >
        <TablerArrowDown />
      </button>
    </div>

    <label class=":uno: absolute right-1 top-1 cursor-pointer">
      <MingCloseCircle class=":uno: text-gray-700" @click="handleRemoveClick" />
    </label>
  </div>
</template>