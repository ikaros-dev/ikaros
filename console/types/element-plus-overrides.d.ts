import "element-plus";

declare module "element-plus" {
  interface DrawerProps {
    modelValue?: boolean | Record<string, unknown> | null;
  }
}
