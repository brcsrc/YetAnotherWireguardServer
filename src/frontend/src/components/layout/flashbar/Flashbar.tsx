import { Flashbar as CloudscapeFlashbar } from "@cloudscape-design/components";
import { useFlashbarContext } from "../../../context/FlashbarContextProvider";
import "./Flashbar.css";

// Rendered as a fixed overlay (see Flashbar.css .flashbar-overlay) instead of
// AppLayout's notifications slot. this forces flashbar items to appear on top
// instead of pushing content down
const Flashbar = () => {
  const { flashbarItems } = useFlashbarContext();

  return (
    <div className="flashbar-overlay">
      <CloudscapeFlashbar items={flashbarItems} stackItems={true} />
    </div>
  );
};

export default Flashbar;
