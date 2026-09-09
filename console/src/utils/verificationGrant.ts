let currentGrant = "";

export function setVerificationGrant(value: string) {
  currentGrant = value;
}

export function getVerificationGrant() {
  return currentGrant;
}

export function clearVerificationGrant() {
  currentGrant = "";
}
