import { axiosInstance } from "@halo-dev/api-client";
import {
  ConsoleApiMomentHaloRunV1alpha1MomentApi,
  MomentV1alpha1Api,
  UcApiMomentHaloRunV1alpha1MomentApi,
} from "./generated";
import type { Stats } from "./generated";

const momentsCoreApiClient = {
  moment: new MomentV1alpha1Api(undefined, "", axiosInstance),
};

const updateMomentUpvote = (name: string, upvote: number) =>
  axiosInstance
    .put<Stats>(
      `/apis/console.api.moment.halo.run/v1alpha1/moments/${encodeURIComponent(name)}/upvote`,
      { upvote }
    )
    .then((response) => response.data);

const momentsConsoleApiClient = {
  moment: new ConsoleApiMomentHaloRunV1alpha1MomentApi(undefined, "", axiosInstance),
  upvote: {
    updateMomentUpvote,
  },
};

const momentsUcApiClient = {
  moment: new UcApiMomentHaloRunV1alpha1MomentApi(undefined, "", axiosInstance),
};

export { momentsConsoleApiClient, momentsCoreApiClient, momentsUcApiClient };