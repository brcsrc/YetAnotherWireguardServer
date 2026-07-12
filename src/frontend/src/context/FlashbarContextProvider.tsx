import * as React from "react";
import type { FlashbarProps } from "@cloudscape-design/components";
import { ButtonProps } from "@cloudscape-design/components";
import { v4 as uuid4 } from "uuid";
import MessageDefinition = FlashbarProps.MessageDefinition;

export interface FlashbarItem {
  header?: React.ReactNode;
  content?: React.ReactNode;
  dismissLabel?: string;
  statusIconAriaLabel?: string;
  loading?: boolean;
  type?: FlashbarProps.Type;
  ariaRole?: FlashbarProps.AriaRole;
  action?: React.ReactNode;
  buttonText?: ButtonProps["children"];
  onButtonClick?: ButtonProps["onClick"];
  duration?: number;
}

interface FlashbarContextType {
  flashbarItems: MessageDefinition[];
  addFlashbarItem: (item: FlashbarItem) => string;
  removeFlashbarITemById: (id: string) => void;
}

const FlashbarContext = React.createContext({
  flashbarItems: [],
  addFlashbarItem: (item: FlashbarItem) => "",
  removeFlashbarItemById: () => {},
});

interface FlashbarContextProviderProps {
  children?: React.ReactNode;
}
export const FlashbarContextProvider = (props: FlashbarContextProviderProps) => {
  const [flashbarItems, setFlashbarItems] = React.useState([]);

  const removeFlashbarItemById = (id: string) =>
    setFlashbarItems(flashbarItems.filter((i) => i.id != id));

  const addFlashbarItem = (item: FlashbarItem): string => {
    const itemId = uuid4();
    const itemWithIdAndDismissHandler = {
      ...item,
      id: itemId,
      dismissible: true,
      onDismiss: () => {
        removeFlashbarItemById(itemId);
      },
    };
    setFlashbarItems((prevItems) => [...prevItems, itemWithIdAndDismissHandler]);

    if (item.duration && item.duration > 0) {
      setTimeout(() => {
        removeFlashbarItemById(itemId);
      }, item.duration);
    }
    return itemId;
  };

  return (
    <FlashbarContext.Provider value={{ flashbarItems, addFlashbarItem, removeFlashbarItemById }}>
      {props.children}
    </FlashbarContext.Provider>
  );
};

export const useFlashbarContext = () => {
  return React.useContext(FlashbarContext);
};
