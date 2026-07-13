import { useLocation, useNavigate } from "react-router";
import { BreadcrumbGroup } from "@cloudscape-design/components";

const Breadcrumbs = () => {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  // without onFollow, breadcrumb links do a hard reload of the destination when clicked
  const onFollow = (event) => {
    if (!event.detail.external) {
      event.preventDefault();
      navigate(event.detail.href);
    }
  };

  if (pathname === "/") {
    return <BreadcrumbGroup items={[]} onFollow={onFollow} />;
  }
  const pathElements = pathname.split("/").filter(Boolean);
  let breadcrumbs = pathElements.map((element, index) => {
    return {
      text: element.charAt(0).toUpperCase() + element.slice(1),
      href: "/" + pathElements.slice(0, index + 1).join("/"),
    };
  });
  return <BreadcrumbGroup items={breadcrumbs} onFollow={onFollow} />;
};
export default Breadcrumbs;
