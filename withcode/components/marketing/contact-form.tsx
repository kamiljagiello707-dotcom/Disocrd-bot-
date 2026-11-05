"use client";
import { useEffect, useState, useTransition } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import Script from "next/script";
import { contactSchema, contactTopics, type ContactInput } from "@/lib/validators/contact";
import { submitContact } from "@/lib/actions/contact";
import { Button } from "@/components/ui/button";
import { FieldError, Input, Label, Textarea } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { toast } from "@/components/ui/toast";
import { CheckCircle } from "@/components/ui/icons";

export function ContactForm({ turnstileSiteKey }: { turnstileSiteKey?: string }) {
  const t = useTranslations("contact");
  const tv = useTranslations("validation");
  const [pending, start] = useTransition();
  const [done, setDone] = useState(false);
  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<ContactInput>({
    resolver: zodResolver(contactSchema),
    defaultValues: {
      name: "",
      email: "",
      message: "",
      website: "",
      topic: "SUPPORT",
    },
  });

  // temat z linku (np. /contact?topic=PREMIUM) — strona jest statyczna, więc czytamy URL po hydratacji
  useEffect(() => {
    const topic = new URLSearchParams(window.location.search).get("topic");
    if (contactTopics.includes(topic as (typeof contactTopics)[number]))
      setValue("topic", topic as ContactInput["topic"]);
  }, [setValue]);

  const msg = (field: keyof ContactInput) => {
    const e = errors[field];
    if (!e) return undefined;
    if (field === "email") return tv("email");
    if (e.type === "too_small") return tv("min", { min: field === "message" ? 20 : 2 });
    if (e.type === "too_big") return tv("max", { max: field === "message" ? 5000 : 80 });
    return tv("required");
  };

  const onSubmit = handleSubmit((values) =>
    start(async () => {
      const token = (
        document.querySelector('[name="cf-turnstile-response"]') as HTMLInputElement | null
      )?.value;
      const res = await submitContact({ ...values, ...(token ? { turnstileToken: token } : {}) });
      if (res.ok) {
        setDone(true);
        toast.success(t("success"));
      } else if (res.error === "rate_limited") toast.error(t("rateLimited"));
      else if (res.error === "captcha") toast.error(tv("captcha"));
      else toast.error(t("error"));
    }),
  );

  if (done) {
    return (
      <div className="glass flex flex-col items-start gap-4 rounded-xl p-8" role="status">
        <CheckCircle className="size-8 text-success" />
        <p className="text-lg text-white">{t("success")}</p>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} noValidate className="glass space-y-5 rounded-xl p-6 md:p-8">
      <div className="grid gap-5 sm:grid-cols-2">
        <div>
          <Label htmlFor="c-name">{t("name")}</Label>
          <Input
            id="c-name"
            autoComplete="name"
            aria-invalid={Boolean(errors.name)}
            aria-describedby="c-name-err"
            {...register("name")}
          />
          <FieldError id="c-name-err" message={msg("name")} />
        </div>
        <div>
          <Label htmlFor="c-email">{t("email")}</Label>
          <Input
            id="c-email"
            type="email"
            autoComplete="email"
            aria-invalid={Boolean(errors.email)}
            aria-describedby="c-email-err"
            {...register("email")}
          />
          <FieldError id="c-email-err" message={msg("email")} />
        </div>
      </div>
      <div>
        <Label htmlFor="c-topic">{t("topic")}</Label>
        <Controller
          control={control}
          name="topic"
          render={({ field }) => (
            <Select
              id="c-topic"
              value={field.value}
              onValueChange={(v) =>
                setValue("topic", v as ContactInput["topic"], { shouldValidate: true })
              }
              options={contactTopics.map((x) => ({ value: x, label: t(`topics.${x}`) }))}
            />
          )}
        />
      </div>
      <div>
        <Label htmlFor="c-message">{t("message")}</Label>
        <Textarea
          id="c-message"
          rows={6}
          aria-invalid={Boolean(errors.message)}
          aria-describedby="c-message-err"
          {...register("message")}
        />
        <FieldError id="c-message-err" message={msg("message")} />
      </div>
      {/* honeypot — niewidoczny dla ludzi i czytników ekranu */}
      <div aria-hidden className="absolute -left-[9999px] h-0 w-0 overflow-hidden">
        <label htmlFor="c-website">Website</label>
        <input id="c-website" tabIndex={-1} autoComplete="off" {...register("website")} />
      </div>
      {turnstileSiteKey ? (
        <>
          <Script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer />
          <div className="cf-turnstile" data-sitekey={turnstileSiteKey} data-theme="dark" />
        </>
      ) : null}
      <p className="text-xs text-fg-subtle">{t("privacyNote")}</p>
      <Button type="submit" size="lg" disabled={pending}>
        {pending ? t("sending") : t("send")}
      </Button>
    </form>
  );
}
