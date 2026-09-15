import { ref } from "vue";
import {
  issueStepUpChallenge,
  verifyStepUpChallenge,
  type VerificationMethod
} from "@/api/user";
import { setVerificationGrant } from "@/utils/verificationGrant";

export function useStepUpVerification() {
  const visible = ref(false);
  const loading = ref(false);
  const code = ref("");
  const challengeId = ref("");
  const method = ref<VerificationMethod>("EMAIL_OTP");

  const request = async (
    selectedMethod: VerificationMethod,
    onReused: (grant: string) => Promise<void>
  ) => {
    loading.value = true;
    try {
      method.value = selectedMethod;
      const challenge = await issueStepUpChallenge(selectedMethod);
      if (challenge.verificationGrant) {
        setVerificationGrant(challenge.verificationGrant);
        await onReused(challenge.verificationGrant);
        return;
      }
      challengeId.value = challenge.id;
      code.value = "";
      visible.value = true;
    } finally {
      loading.value = false;
    }
  };

  const verify = async () => {
    if (!challengeId.value || !/^\d{6}$/.test(code.value)) return null;
    loading.value = true;
    try {
      const result = await verifyStepUpChallenge(challengeId.value, code.value, method.value);
      setVerificationGrant(result.verificationGrant);
      return result.verificationGrant;
    } finally {
      loading.value = false;
    }
  };

  const close = () => {
    visible.value = false;
    code.value = "";
    challengeId.value = "";
  };

  return { visible, loading, code, challengeId, method, request, verify, close };
}
