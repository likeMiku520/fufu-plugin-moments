<script lang="ts" setup>
import { momentsConsoleApiClient, momentsCoreApiClient } from "@/api";
import type { JsonPatchInner, Moment, MomentMedia, MomentMediaTypeEnum } from "@/api/generated";
import MediaCard from "@/components/MediaCard.vue";
import ReleaseTimePicker from "@/components/ReleaseTimePicker.vue";
import { useConsoleTagQueryFetch } from "@/composables/use-tag";
import "@/styles/upvote-picker.scss";
import type { MediaMoveDirection } from "@/types";
import { IconEye, IconEyeOff, Toast, VButton, VLoading } from "@halo-dev/components";
import type { AttachmentLike } from "@halo-dev/ui-shared";
import { useQueryClient } from "@tanstack/vue-query";
import { cloneDeep } from "es-toolkit";
import { PopoverContent, PopoverRoot, PopoverTrigger } from "reka-ui";
import { computed, defineAsyncComponent, onMounted, ref, toRaw, watch } from "vue";
import SendMoment from "~icons/ic/sharp-send";
import TablerHeart from "~icons/tabler/heart";
import TablerPhoto from "~icons/tabler/photo";

const TextEditor = defineAsyncComponent({
  loader: () => import("@/components/TextEditor.vue"),
  loadingComponent: VLoading,
});

const props = withDefaults(
  defineProps<{
    moment?: Moment;
    upvote?: number;
  }>(),
  {
    moment: undefined,
    upvote: undefined,
  }
);

const emit = defineEmits<{
  (event: "update"): void;
  (event: "cancel"): void;
}>();

const queryClient = useQueryClient();

const initMoment: Moment = {
  spec: {
    content: {
      raw: "",
      html: "",
      medium: [],
    },
    releaseTime: new Date().toISOString(),
    owner: "",
    // @unocss-skip-start
    visible: "PUBLIC",
    // @unocss-skip-end
    tags: [],
    approved: true,
  },
  metadata: {
    generateName: "moment-",
    name: "",
  },
  kind: "Moment",
  apiVersion: "moment.halo.run/v1alpha1",
};

onMounted(() => {
  if (props.moment) {
    formState.value = cloneDeep(props.moment);
    if (props.moment.spec.releaseTime) {
      releaseTime.value = new Date(props.moment.spec.releaseTime);
    }
  }
});

// 用户选择的发布时间，null 表示未设置（发布时自动填充当前时间）
const releaseTime = ref<Date | null>(null);

const releaseTimeChanged = computed(() => {
  if (!releaseTime.value) {
    return false;
  }
  const origin = props.moment?.spec.releaseTime;
  return !origin || new Date(origin).getTime() !== releaseTime.value.getTime();
});

const formState = ref<Moment>(cloneDeep(initMoment));
const saving = ref<boolean>(false);
const attachmentSelectorModal = ref(false);
const isUpdateMode = computed(() => !!formState.value.metadata.creationTimestamp);
const isEditorEmpty = ref<boolean>(true);

const originalUpvote = computed(() => props.upvote ?? 0);
const upvoteCount = ref<number>(0);
const upvotePopoverOpen = ref<boolean>(false);
const upvoteDraft = ref<string>("0");

watch(
  () => props.upvote,
  (value) => {
    if (!upvotePopoverOpen.value) {
      upvoteCount.value = value ?? 0;
    }
  },
  { immediate: true }
);

const upvoteChanged = computed(() => upvoteCount.value !== originalUpvote.value);

const parsedUpvoteDraft = computed(() => {
  const value = Number(upvoteDraft.value);
  return Number.isInteger(value) && value >= 0 ? value : null;
});

function handleUpvotePopoverOpen(value: boolean) {
  if (value) {
    upvoteDraft.value = String(upvoteCount.value);
  }
  upvotePopoverOpen.value = value;
}

function applyUpvoteCount() {
  if (parsedUpvoteDraft.value === null) {
    return;
  }
  upvoteCount.value = parsedUpvoteDraft.value;
  upvotePopoverOpen.value = false;
}

const syncUpvote = async (momentName: string, upvote: number) => {
  await momentsConsoleApiClient.upvote.updateMomentUpvote(momentName, upvote);
};

const handlerCreateOrUpdateMoment = async () => {
  if (saveDisable.value) {
    return;
  }
  try {
    saving.value = true;
    queryEditorTags();
    if (isUpdateMode.value) {
      handleUpdate();
    } else {
      handleSave(formState.value);
      handleReset();
    }
  } catch (error) {
    console.error(error);
  } finally {
    saving.value = false;
  }
};

const handleSave = async (moment: Moment) => {
  const upvoteToSync = upvoteCount.value;
  moment.spec.releaseTime = releaseTime.value?.toISOString() ?? new Date().toISOString();
  moment.spec.approved = true;

  const { data: created } = await momentsConsoleApiClient.moment.createMoment({
    moment: moment,
  });

  const createdName = created.metadata.name;
  if (createdName && upvoteToSync > 0) {
    await syncUpvote(createdName, upvoteToSync);
  }

  queryClient.invalidateQueries({ queryKey: ["plugin:moments:list"] });

  Toast.success("发布成功");
};

const handleUpdate = async () => {
  const jsonPatchInner: JsonPatchInner[] = [
    {
      op: "add",
      path: "/spec/tags",
      value: formState.value.spec.tags || [],
    },
    {
      op: "add",
      path: "/spec/content",
      value: formState.value.spec.content,
    },
    {
      op: "add",
      path: "/spec/visible",
      value: formState.value.spec.visible || false,
    },
  ];

  if (releaseTimeChanged.value && releaseTime.value) {
    jsonPatchInner.push({
      op: "add",
      path: "/spec/releaseTime",
      value: releaseTime.value.toISOString(),
    });
  }

  await momentsCoreApiClient.moment.patchMoment({
    name: formState.value.metadata.name,
    jsonPatchInner,
  });

  const momentName = formState.value.metadata.name;
  if (momentName && upvoteChanged.value) {
    await syncUpvote(momentName, upvoteCount.value);
  }

  emit("update");

  queryClient.invalidateQueries({ queryKey: ["plugin:moments:list"] });

  Toast.success("发布成功");
};

const parse = new DOMParser();
const queryEditorTags = function () {
  const tags: Set<string> = new Set();
  const document: Document = parse.parseFromString(formState.value.spec.content.raw!, "text/html");
  const nodeList: NodeList = document.querySelectorAll("a.tag");
  if (nodeList) {
    for (const tagNode of nodeList) {
      if (tagNode.textContent) {
        tags.add(tagNode.textContent);
      }
    }
  }
  formState.value.spec.tags = Array.from(tags);
};

const handleReset = () => {
  formState.value = toRaw(cloneDeep(initMoment));
  releaseTime.value = null;
  isEditorEmpty.value = true;
  upvoteCount.value = 0;
  sortingMediaIndex.value = null;
};

const supportImageTypes: string[] = [
  "image/apng",
  "image/avif",
  "image/bmp",
  "image/gif",
  "image/x-icon",
  "image/jpg",
  "image/jpeg",
  "image/png",
  "image/svg+xml",
  "image/tiff",
  "image/webp",
];

const supportVideoTypes: string[] = ["video/*"];

const supportAudioTypes: string[] = ["audio/*"];

const accepts = [...supportImageTypes, ...supportVideoTypes, ...supportAudioTypes];

const mediumWhitelist: Map<string, MomentMediaTypeEnum> = new Map([
  ["image", "PHOTO"],
  ["video", "VIDEO"],
  ["audio", "AUDIO"],
]);

const onAttachmentsSelect = async (attachments: AttachmentLike[]) => {
  const medias: {
    url: string;
    cover?: string;
    displayName?: string;
    type?: string;
  }[] = attachments
    .map((attachment) => {
      if (typeof attachment === "string") {
        return {
          url: attachment,
          cover: attachment,
        };
      }
      if ("url" in attachment) {
        return {
          url: attachment.url,
          cover: attachment.url,
        };
      }
      if ("spec" in attachment) {
        return {
          url: attachment.status?.permalink,
          cover: attachment.status?.permalink,
          displayName: attachment.spec.displayName,
          type: attachment.spec.mediaType,
        };
      }
    })
    .filter(Boolean) as {
    url: string;
    cover?: string;
    displayName?: string;
    type?: string;
  }[];
  if (!formState.value.spec.content.medium) {
    formState.value.spec.content.medium = [];
  }
  medias.forEach((media) => {
    if (!addMediumVerify(media)) {
      return false;
    }
    if (!media.type) {
      return false;
    }
    const fileType = media.type.split("/")[0];
    formState.value.spec.content.medium?.push({
      type: mediumWhitelist.get(fileType),
      url: media.url,
      originType: media.type,
    } as MomentMedia);
  });
};

const saveDisable = computed(() => {
  const medium = formState.value.spec.content.medium;
  if (medium !== undefined && medium.length > 0 && medium.length <= 9) {
    return false;
  }
  if (!isEditorEmpty.value) {
    return false;
  }

  if (isUpdateMode.value) {
    const oldVisible = props.moment?.spec.visible;
    if (oldVisible != formState.value.spec.visible) {
      return false;
    }
    if (releaseTimeChanged.value) {
      return false;
    }
    if (upvoteChanged.value) {
      return false;
    }
  }

  return true;
});

const removeMedium = (media: MomentMedia) => {
  const formMedium = formState.value.spec.content.medium;
  if (!formMedium) {
    return;
  }
  const index: number = formMedium.indexOf(media);
  if (index > -1) {
    formMedium.splice(index, 1);
  }
  sortingMediaIndex.value = null;
};

const MEDIA_GRID_COLUMNS = 3;
const sortingMediaIndex = ref<number | null>(null);

const toggleMediaSorting = (index: number) => {
  sortingMediaIndex.value = sortingMediaIndex.value === index ? null : index;
};

const mediaTargetIndex = (index: number, direction: MediaMoveDirection, total: number) => {
  switch (direction) {
    case "up":
      return index - MEDIA_GRID_COLUMNS >= 0 ? index - MEDIA_GRID_COLUMNS : null;
    case "down":
      return index + MEDIA_GRID_COLUMNS < total ? index + MEDIA_GRID_COLUMNS : null;
    case "left":
      return index > 0 && index % MEDIA_GRID_COLUMNS !== 0 ? index - 1 : null;
    case "right":
      return index < total - 1 && (index + 1) % MEDIA_GRID_COLUMNS !== 0 ? index + 1 : null;
    default:
      return null;
  }
};

const moveMedium = (index: number, direction: MediaMoveDirection) => {
  const formMedium = formState.value.spec.content.medium;
  if (!formMedium) {
    return;
  }
  const targetIndex = mediaTargetIndex(index, direction, formMedium.length);
  if (targetIndex === null) {
    return;
  }
  const current = formMedium[index];
  formMedium[index] = formMedium[targetIndex];
  formMedium[targetIndex] = current;
  sortingMediaIndex.value = targetIndex;
};

const mediaDirections = (index: number): MediaMoveDirection[] => {
  const total = formState.value.spec.content.medium?.length || 0;
  return (["up", "down", "left", "right"] as MediaMoveDirection[]).filter(
    (direction) => mediaTargetIndex(index, direction, total) !== null
  );
};

const handlerCancel = () => {
  emit("cancel");
};

const uploadMediumNum = 9;

const addMediumVerify = (media?: {
  url: string;
  cover?: string;
  displayName?: string;
  type?: string;
}) => {
  const formMedium = formState.value.spec.content.medium;
  if (!formMedium || formMedium.length == 0) {
    return true;
  }

  if (formMedium.length >= uploadMediumNum) {
    Toast.warning("最多允许添加 " + uploadMediumNum + " 个附件");
    return false;
  }

  if (media) {
    if (formState.value.spec.content.medium?.filter((item) => item.url == media.url).length != 0) {
      Toast.warning("已过滤重复添加的附件");
      return false;
    }
  }

  return true;
};

function handleToggleVisible() {
  // @unocss-skip-start
  const { visible: currentVisible } = formState.value.spec;
  // @unocss-skip-end
  formState.value.spec.visible = currentVisible === "PUBLIC" ? "PRIVATE" : "PUBLIC";
}

function handleKeydown(event: KeyboardEvent) {
  if (event.ctrlKey && event.key === "Enter") {
    handlerCreateOrUpdateMoment();
    return false;
  }
}
</script>

<template>
  <div class=":uno: card shrink overflow-hidden border rounded-md bg-white">
    <AttachmentSelectorModal
      v-if="attachmentSelectorModal"
      v-permission="['system:attachments:view']"
      :min="1"
      :max="9"
      :accepts="accepts"
      @select="onAttachmentsSelect"
      @close="attachmentSelectorModal = false"
    />
    <TextEditor
      v-model:raw="formState.spec.content.raw"
      v-model:html="formState.spec.content.html"
      v-model:isEmpty="isEditorEmpty"
      :tag-query-fetch="useConsoleTagQueryFetch"
      class=":uno: min-h-[9rem]"
      tabindex="-1"
      @keydown="handleKeydown"
    />
    <div v-if="formState.spec.content.medium?.length" class=":uno: img-box flex px-3.5 py-2">
      <ul class=":uno: grid grid-cols-3 w-full gap-1.5 sm:w-1/2" role="list">
        <li
          v-for="(media, index) in formState.spec.content.medium"
          :key="media.url"
          class=":uno: inline-block overflow-hidden border rounded-md"
        >
          <MediaCard
            :media="media"
            sortable
            :sorting="sortingMediaIndex === index"
            :directions="mediaDirections(index)"
            @remove="removeMedium"
            @toggle-sorting="toggleMediaSorting(index)"
            @move="(direction) => moveMedium(index, direction)"
          ></MediaCard>
        </li>
      </ul>
    </div>
    <div class=":uno: flex flex-wrap justify-between gap-2 bg-white px-3.5 py-2">
      <div class=":uno: h-fit">
        <button
          type="button"
          class=":uno: group flex cursor-pointer items-center justify-center rounded-full p-2 hover:bg-sky-600/10"
          @click="addMediumVerify() && (attachmentSelectorModal = true)"
        >
          <TablerPhoto class=":uno: size-full text-base text-gray-600 group-hover:text-sky-600" />
        </button>
      </div>

      <div class=":uno: flex flex-wrap items-center justify-end gap-2">
        <div v-permission="['plugin:moments:manage']" class=":uno: h-fit">
          <PopoverRoot :open="upvotePopoverOpen" @update:open="handleUpvotePopoverOpen">
            <PopoverTrigger as-child>
              <button
                type="button"
                class=":uno: moments-upvote-trigger group inline-flex cursor-pointer items-center justify-center gap-1.5 rounded-full p-2"
                :title="`点赞数量：${upvoteCount}`"
                :aria-label="`修改点赞数量，当前 ${upvoteCount}`"
              >
                <TablerHeart class=":uno: size-4 shrink-0" />
                <span class=":uno: whitespace-nowrap text-xs tabular-nums">{{ upvoteCount }}</span>
              </button>
            </PopoverTrigger>
            <PopoverContent
              class=":uno: moments-upvote-popover"
              align="end"
              :side-offset="6"
              :collision-padding="8"
              aria-label="修改点赞数量"
            >
              <label class=":uno: text-xs text-gray-600" for="moments-upvote-input">
                点赞数量
              </label>
              <input
                id="moments-upvote-input"
                v-model="upvoteDraft"
                class=":uno: moments-upvote-input"
                type="number"
                min="0"
                step="1"
                inputmode="numeric"
                @keydown.enter.prevent="applyUpvoteCount"
              />
              <p v-if="parsedUpvoteDraft === null" class=":uno: moments-upvote-error" role="alert">
                请输入不小于 0 的整数
              </p>
              <div class=":uno: moments-upvote-actions">
                <VButton size="sm" @click="upvotePopoverOpen = false">取消</VButton>
                <VButton
                  size="sm"
                  type="secondary"
                  :disabled="parsedUpvoteDraft === null"
                  @click="applyUpvoteCount"
                >
                  确定
                </VButton>
              </div>
            </PopoverContent>
          </PopoverRoot>
        </div>

        <ReleaseTimePicker v-model="releaseTime" />

        <div
          v-tooltip="{
            content: formState.spec.visible === 'PRIVATE' ? `私有访问` : '公开访问',
          }"
          class=":uno: group flex cursor-pointer items-center justify-center rounded-full p-2"
          :class="
            formState.spec.visible === 'PRIVATE'
              ? ':uno: hover:bg-red-600/10'
              : ':uno: hover:bg-green-600/10'
          "
          @click="handleToggleVisible()"
        >
          <IconEyeOff
            v-if="formState.spec.visible === 'PRIVATE'"
            class=":uno: size-full text-base text-gray-600 group-hover:text-red-600"
          />
          <IconEye
            v-else
            class=":uno: size-full text-base text-gray-600 group-hover:text-green-600"
          />
        </div>

        <button
          v-if="isUpdateMode"
          class=":uno: h-7 inline-flex cursor-pointer items-center rounded px-3 text-gray-600 hover:bg-sky-600/10 hover:text-sky-600"
          @click="handlerCancel"
        >
          <span class=":uno: text-xs"> 取消 </span>
        </button>

        <div
          v-permission="['plugin:moments:manage', 'uc:plugin:moments:publish']"
          class=":uno: h-fit"
        >
          <VButton
            v-model:disabled="saveDisable"
            :loading="saving"
            size="sm"
            type="primary"
            @click="handlerCreateOrUpdateMoment"
          >
            <template #icon>
              <SendMoment class=":uno: size-full scale-[1.35]" />
            </template>
          </VButton>
        </div>
      </div>
    </div>
  </div>
</template>
