import type { MDXComponents } from "mdx/types";
import { CodeBlock } from "@/components/docs/code-block";

const components: MDXComponents = {
  pre: (props) => <CodeBlock {...props} />,
  table: (props) => (
    <div className="overflow-x-auto rounded-lg border border-border">
      <table {...props} />
    </div>
  ),
};

export function useMDXComponents(): MDXComponents {
  return components;
}
