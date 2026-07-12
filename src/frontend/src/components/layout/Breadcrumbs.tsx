import { useLocation } from "react-router";
import { BreadcrumbGroup } from "@cloudscape-design/components";

const Breadcrumbs = () => {
  const { pathname } = useLocation();
  if (pathname === "/") {
    return <BreadcrumbGroup items={[]} />;
  }
  const pathElements = pathname.split("/").filter(Boolean);
  let breadcrumbs = pathElements.map((element, index) => {
    return {
      text: element.charAt(0).toUpperCase() + element.slice(1),
      href: "/" + pathElements.slice(0, index + 1).join("/"),
    };
  });
  return <BreadcrumbGroup items={breadcrumbs} />;
};
export default Breadcrumbs;
