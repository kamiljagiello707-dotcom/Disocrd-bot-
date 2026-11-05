"use client";
import { useState, useTransition } from "react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { revokeCase } from "@/lib/actions/dashboard";
import { Button } from "@/components/ui/button";
import { Modal, ModalContent } from "@/components/ui/modal";
import { Textarea } from "@/components/ui/input";
import { toast } from "@/components/ui/toast";

export function RevokeButton({ guildId, caseId }: { guildId: string; caseId: string }) {
  const t = useTranslations("dashboard.cases");
  const tc = useTranslations("common");
  const td = useTranslations("dashboard");
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [pending, start] = useTransition();
  return (
    <>
      <Button variant="danger" onClick={() => setOpen(true)}>
        {t("revoke")}
      </Button>
      <Modal open={open} onOpenChange={setOpen}>
        <ModalContent title={t("revoke")} description={t("revokeConfirm")} closeLabel={tc("close")}>
          <label htmlFor="revoke-reason" className="mb-2 block text-sm text-white">
            {t("revokeReason")}
          </label>
          <Textarea
            id="revoke-reason"
            maxLength={500}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
          <div className="mt-5 flex justify-end gap-2">
            <Button variant="ghost" onClick={() => setOpen(false)}>
              {tc("cancel")}
            </Button>
            <Button
              variant="danger"
              disabled={pending}
              onClick={() =>
                start(async () => {
                  const res = await revokeCase(guildId, { caseId, ...(reason ? { reason } : {}) });
                  if (res.ok) {
                    toast.success(t("revokedToast"));
                    setOpen(false);
                    router.refresh();
                  } else toast.error(td("saveError"), res.error);
                })
              }
            >
              {tc("confirm")}
            </Button>
          </div>
        </ModalContent>
      </Modal>
    </>
  );
}
