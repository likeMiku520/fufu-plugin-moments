<script lang="ts" setup>
import { momentsCoreApiClient } from "@/api";
import type { ListedMoment, Moment } from "@/api/generated";
import {
  Dialog,
  IconEyeOff,
  Toast,
  VAvatar,
  VDropdown,
  VDropdownItem,
  VStatusDot,
} from "@halo-dev/components";
import { utils } from "@halo-dev/ui-shared";
import { useQueryClient } from "@tanstack/vue-query";
import { computed, ref } from "vue";
import LucideMoreHorizontal from "~icons/lucide/more-horizontal";
import TablerClock from "~icons/tabler/clock";
import MomentEdit from "./MomentEdit.vue";
import MomentPreview from "./MomentPreview.vue";

const props = withDefaults(
  defineProps<{
    listedMoment: ListedMoment;
    editing?: boolean;
  }>(),
  {
    editing: false,
  }
);

const emit = defineEmits<{
  (event: "save", moment: Moment): void;
  (event: "update", moment: Moment): void;
  (event: "remove"): void;
}>();

const queryClient = useQueryClient();

const editing = ref(props.editing);
const owner = computed(() => props.listedMoment?.owner);

const scheduled = computed(() => {
  const releaseTime = props.listedMoment?.moment.spec.releaseTime;
  return !!releaseTime && new Date(releaseTime).getTime() > Date.now();
});

const deleteMoment = () => {
  Dialog.warning({
    title: "确定要删除该瞬间吗？",
    description: "该操作不可逆",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        await momentsCoreApiClient.moment.deleteMoment({
          name: props.listedMoment.moment.metadata.name,
        });

        Toast.success("删除成功");
        emit("remove");
      } catch (error) {
        console.error("Failed to delete comment", error);
      }
    },
  });
};

const onUpdated = () => {
  editing.value = false;
};

const handleApproved = async () => {
  await momentsCoreApiClient.moment.patchMoment({
    name: props.listedMoment.moment.metadata.name,
    jsonPatchInner: [
      {
        op: "add",
        path: "/spec/approved",
        value: true,
      },
    ],
  });

  queryClient.invalidateQueries({ queryKey: ["plugin:moments:list"] });
};
</script>
<template>
  <div>
    <div class=":uno: card preview relative shrink border-t border-gray-100 bg-white py-6">
      <div class=":uno: flex items-start gap-3">
        <VAvatar
          :alt="owner?.displayName"
          :src="owner?.avatar"
          size="md"
          circle
          class=":uno: flex-none"
        ></VAvatar>
        <div class=":uno: min-w-0 flex-1 shrink">
          <div class=":uno: flex items-center justify-between">
            <div class=":uno: flex items-center space-x-3">
              <div>
                <b> {{ owner?.displayName }} </b>
              </div>
              <div
                v-if="listedMoment?.moment.spec.visible == 'PRIVATE'"
                v-tooltip="{
                  content: '私有访问',
                }"
              >
                <IconEyeOff class=":uno: text-xs text-gray-500" />
              </div>
              <div>
                <VStatusDot
                  v-show="listedMoment?.moment.spec.approved === false"
                  class=":uno: mr-2 cursor-default"
                  state="success"
                  animate
                >
                  <template #text>
                    <span class=":uno: text-xs text-gray-500">
                      {{ `待审核` }}
                    </span>
                  </template>
                </VStatusDot>
              </div>
            </div>

            <div class=":uno: flex items-center">
              <div
                v-if="scheduled"
                v-tooltip="{
                  content: `定时发布：${utils.date.format(
                    listedMoment?.moment.spec.releaseTime
                  )}`,
                }"
                class=":uno: mr-2 flex cursor-default items-center gap-1 text-xs text-gray-500"
              >
                <TablerClock class=":uno: size-3.5" />
                <span>定时发布</span>
              </div>
              <div class=":uno: mr-2 cursor-default text-xs text-gray-500">
                <span
                  v-tooltip="{
                    content: utils.date.format(listedMoment?.moment.spec.releaseTime),
                  }"
                >
                  {{ utils.date.timeAgo(listedMoment?.moment.spec.releaseTime) }}
                </span>
              </div>
              <VDropdown v-permission="['plugin:moments:manage']" compute-transform-origin>
                <div
                  class=":uno: group flex cursor-pointer items-center justify-center rounded-full p-2 hover:bg-sky-600/10"
                >
                  <LucideMoreHorizontal
                    class=":uno: size-full cursor-pointer text-base text-gray-600 group-hover:text-sky-600"
                  />
                </div>
                <template #popper>
                  <VDropdownItem v-if="!listedMoment?.moment.spec.approved" @click="handleApproved">
                    审核通过
                  </VDropdownItem>
                  <VDropdownItem @click="editing = true"> 编辑 </VDropdownItem>
                  <VDropdownItem type="danger" @click="deleteMoment"> 删除 </VDropdownItem>
                </template>
              </VDropdown>
            </div>
          </div>

          <div class=":uno: mt-3">
            <MomentEdit
              v-if="editing"
              :moment="listedMoment?.moment"
              :upvote="listedMoment?.stats?.upvote"
              @update="onUpdated"
              @cancel="editing = false"
            ></MomentEdit>
            <MomentPreview
              v-else
              :uc="false"
              :moment="listedMoment"
              @switch-edit-mode="editing = true"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
